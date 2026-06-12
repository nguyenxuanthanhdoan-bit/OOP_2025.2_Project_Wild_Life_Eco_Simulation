package model.living_beings.animal;

import model.living_beings.Human;
import core.Vector2;
import model.items.Carcass;
import model.items.FoodSource;
import model.plants.Plant;
import model.strategies.StrategySelector;
import model.living_beings.AnimalProfile;
import model.living_beings.DietType;
import model.living_beings.LivingBeing;
import model.entity.Entity;
import core.GameConfig;
import model.world.WorldEventType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import model.entity.Entity;

/**
 * Lớp cơ sở trừu tượng cho mọi loài động vật trong hệ sinh thái.
 * Cây kế thừa: Entity → LivingBeing → Animal → (HerbivoreAnimal / CarnivoreAnimal)
 */
public abstract class Animal extends LivingBeing {

    // =========================================================
    // HẰNG SỐ NGƯỠNG AI
    // =========================================================

    public static final double HUNGER_WARNING_THRESHOLD   = 0.50;
    public static final double THIRST_WARNING_THRESHOLD   = 0.50;
    public static final double CRITICAL_SURVIVAL_THRESHOLD = 0.05; // Ngưỡng nguy hiểm tính mạng (5%)

    // =========================================================
    // THUỘC TÍNH
    // =========================================================

    protected String speciesName;

    // Sinh tồn (VitalStatsComponent)
    protected VitalStatsComponent vitals;
    protected SensoryComponent sensory;
    protected StateComponent state;
    protected ReproductionComponent reproduction;

    // Giác quan & trạng thái
    
    protected boolean alive;
    protected DietType dietType;
    protected AnimalProfile profile;
    private final Map<UUID, Float> unsafeFoodMemory = new HashMap<>();
    protected float damageBlinkTimer = 0.0f;

    // Caching for threats
    
    
    
    

    // Cache to avoid object allocation in update
    private final Vector2 oldPosCache = new Vector2(0, 0);

    // Pre-calculated escape directions to avoid Math.cos/sin
    private static final Vector2[] ESCAPE_DIRS = new Vector2[8];
    static {
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            ESCAPE_DIRS[i] = new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
        }
    }

    /** Tham chiếu tới World hiện tại — được World.update() gán trước mỗi frame. */
    protected model.world.World worldRef = null;
    public void setWorldRef(model.world.World w) { this.worldRef = w; }

    // =========================================================
    // STUCK DETECTOR — Phát hiện và thoát khẩn cấp khi bị kẹt
    // =========================================================
    private final model.navigation.StuckDetector stuckDetector = new model.navigation.StuckDetector();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /** Constructor đầy đủ tham số. */
    public Animal(
            Vector2 position, float size, float baseSpeed,
            String speciesName,
            double maxHealth, double maxHunger, double hungerDecayRate,
            double maxThirst, double thirstDecayRate,
            double maxAge, double visionRange, DietType dietType
    ) {
        super(position, size, baseSpeed);
        this.speciesName = speciesName;
        this.vitals = new VitalStatsComponent(this);
        this.sensory = new SensoryComponent(this, 100);
        this.state = new StateComponent(this);
        this.reproduction = new ReproductionComponent(this);
        this.vitals.setMaxHealth(maxHealth);
        this.vitals.setHealth(maxHealth);
        this.vitals.setMaxHunger(maxHunger);
        this.vitals.setHunger(maxHunger);
        this.vitals.setHungerDecayRate(hungerDecayRate);
        this.vitals.setMaxThirst(maxThirst);
        this.vitals.setThirst(maxThirst);
        this.vitals.setThirstDecayRate(thirstDecayRate);
        this.vitals.setAge(0.0);
        this.vitals.setMaxAge(maxAge);
        this.sensory.setVisionRange(visionRange);
        this.dietType = dietType;
        this.vitals.setAdult(false);
        this.alive = true;
        this.profile = AnimalProfile.defaultFor(dietType);
    }

    /** Constructor rút gọn — lớp con tự gán chỉ số sau khi gọi super(). */
    protected Animal(Vector2 position, float size, float baseSpeed, DietType dietType) {
        super(position, size, baseSpeed);
        this.dietType = dietType;
        this.vitals = new VitalStatsComponent(this);
        this.sensory = new SensoryComponent(this, 100);
        this.state = new StateComponent(this);
        this.reproduction = new ReproductionComponent(this);
        this.vitals.setAge(0.0);
        this.alive = true;
        this.profile = AnimalProfile.defaultFor(dietType);
    }

    // =========================================================
    // VÒNG ĐỜI — update()
    // =========================================================

    @Override
    public void update(float deltaTime) {
        if (!alive) return;

        if (sensory != null) sensory.update(deltaTime);
        if (reproduction != null) reproduction.update(deltaTime);
        if (damageBlinkTimer > 0) {
            damageBlinkTimer -= deltaTime;
        }

        boolean isNight = (worldRef != null) && (worldRef.getTimeOfDay() >= 18.0f || worldRef.getTimeOfDay() <= 5.0f);
        boolean isFish = this.getProfile() != null && this.getProfile().isAquatic();

        // --- STUCK DETECTOR ---
        stuckDetector.update(this, deltaTime);
        // Nếu đang trong chế độ thoát khẩn cấp → bỏ qua strategy thường
        if (stuckDetector.isEscaping()) {
            stuckDetector.doEmergencyEscape(this, deltaTime);
            // Vẫn cập nhật sinh học bình thường
            if (vitals != null) vitals.setHunger(Math.max(0, vitals.getHunger() - (this.getHungerDecayRate() * 1.5f * deltaTime * 0.25f)));
            if (vitals != null) vitals.setThirst(Math.max(0, vitals.getThirst() - (this.getThirstDecayRate() * 1.5f * deltaTime * 0.25f)));
            growOlder(deltaTime);
            if (vitals != null) vitals.setAdult(vitals.getAge() >= vitals.getMaxAge() * 0.2);
            if (vitals != null && vitals.getHunger() <= 0) takeDamage(5.0f * deltaTime);
            if (vitals != null && vitals.getThirst() <= 0) takeDamage(10.0f * deltaTime);
            return;
        }

        decideActiveStrategy();

        oldPosCache.set(this.position);
        
        float moveDeltaTime = deltaTime;
        if (isNight && isFish) {
            moveDeltaTime *= 0.5f; // Cá bơi chậm lại 50% vào ban đêm
        }

        // Kiểm tra xem có đang trên cát và không thích nghi sa mạc
        boolean onSand = false;
        boolean onSnow = false;
        float snowDensity = 0.0f;
        if (worldRef != null && worldRef.getGameMap() != null) {
            onSand = worldRef.getGameMap().isSandTile(this.position.x, this.position.y);
            snowDensity = worldRef.getSnowDensity(this.position);
            onSnow = snowDensity > 0.0f;
        }
        boolean isDesertAdapted = this.getProfile() != null && this.getProfile().isDesertAdapted();

        if (onSand && !isDesertAdapted) {
            moveDeltaTime *= 0.6f; // Giảm tốc độ 40%
        }
        if (onSnow) {
            // Giảm tốc độ tối đa 50% ở nơi tuyết dày đặc nhất
            moveDeltaTime *= (1.0f - (snowDensity * 0.5f));
        }
        
        super.update(moveDeltaTime);

        // Tính hệ số tiêu hao (di chuyển tiêu hao nhiều hơn)
        float distSq = this.position.distanceSquared(oldPosCache);
        if (state != null) state.setMoving(distSq > 0.0001f);
        float decayMultiplier = (state != null && state.isMoving()) ? 1.5f : 1.0f;

        if (onSand && !isDesertAdapted) {
            decayMultiplier *= 2.0f; // Khát nhanh gấp đôi
        }
        if (worldRef != null && worldRef.getCurrentSeason() == model.world.World.Season.WINTER) {
            float winterPenalty = 1.0f + worldRef.getWinterProgress() * 1.5f; // Mùa đông tiêu hao nhanh tối đa gấp 2.5 lần
            if (isFish) {
                winterPenalty *= 2.0f; // Ở dưới nước lạnh hơn, cá đói khát nhanh gấp đôi thú trên bờ
            }
            decayMultiplier *= winterPenalty; 
        }

        // Suy giảm sinh học (× 0.25 để thanh đói/khát tụt chậm hơn 4 lần)
        if (vitals != null) vitals.setHunger(Math.max(0, vitals.getHunger() - (this.getHungerDecayRate() * decayMultiplier * deltaTime * 0.25f)));
        if (vitals != null) vitals.setThirst(Math.max(0, vitals.getThirst() - (this.getThirstDecayRate() * decayMultiplier * deltaTime * 0.25f)));

        growOlder(deltaTime);

        if (vitals != null) vitals.setAdult(vitals.getAge() >= vitals.getMaxAge() * 0.2);

        // Hậu quả đói/khát
        if (vitals != null && vitals.getHunger() <= 0) takeDamage(5.0f * deltaTime);
        if (vitals != null && vitals.getThirst() <= 0) takeDamage(10.0f * deltaTime);
    }

    /** Lấy World hiện tại — override ở subclass nếu cần (mặc định null-safe). */
    protected model.world.World getCurrentWorld() {
        return worldRef; // Được World.update() gán trước mỗi frame
    }

    /**
     * Bộ não AI (Priority Selector): chọn Strategy phù hợp dựa trên trạng thái.
     * Hoạt động cho TẤT CẢ các loài — không lock cứng bất kỳ Strategy nào.
     */
    protected void decideActiveStrategy() {
        model.strategies.IStrategy selected = StrategySelector.select(this);
        if (selected != null && selected != currentStrategy) {
            setStrategy(selected);
        }
    }

    /**
     * Override trả về true ở các lớp con muốn dùng FlockingStrategy
     * khi không có việc gì khác để làm (ví dụ: Rabbit, Elephant).
     */
    protected boolean useFlocking() {
        return getProfile().canFlock();
    }

    /**
     * Lớp con có thể trả về biến thể flocking riêng mà không cần tự đổi strategy trong update().
     */
    protected model.strategies.FlockingStrategy createFlockingStrategy() {
        return new model.strategies.FlockingStrategy();
    }
    protected boolean detectDangerousThreats() {
        return sensory != null && sensory.detectDangerousThreats();
    }
    public boolean hasDangerousThreats() {
        return sensory != null && sensory.hasDangerousThreats();
    }
    public boolean hasGardenThreat() {
        return sensory != null && sensory.hasGardenThreat();
    }
    // =========================================================
    // VÙNG NGUY HIỂM (DANGER ZONE MEMORY)
    // =========================================================
    public void markDangerZone(Entity predator, float radius, float duration) {
        if (sensory != null) sensory.markDangerZone(predator, radius, duration);
    }
    public boolean isInDangerZone(Vector2 pos) {
        return sensory != null && sensory.isInDangerZone(pos);
    }

    // =========================================================
    // HÀNH VI
    // =========================================================

    @Override
    public void move(Vector2 direction, float deltaTime) {
        if (!alive || (state != null && state.isHidden())) return;
        super.move(direction, deltaTime);
    }

    /** Ăn thực vật — hồi điểm đói. */
    public void eat(Plant food) {
        if (vitals != null) vitals.eat(food);
    }

    public boolean canEatPlant(Plant food) {
        return getProfile().canEatPlant(food);
    }

    public boolean canEatFoodSource(FoodSource food) {
        if (!alive || food == null || !food.isAlive()) return false;
        if (!getProfile().canEatMeat()) return false;
        if (food instanceof Carcass) {
            Carcass carcass = (Carcass) food;
            return getProfile().canEatOwnSpecies() || !carcass.getSourceSpecies().equals(this.getSpeciesName());
        }
        return true;
    }

    public boolean canHunt(Animal prey) {
        if (!alive || prey == null || prey == this || !prey.isAlive()) return false;
        if (!getProfile().canHunt()) return false;
        if (prey.isHidden()) return false;
        if (!prey.canBeHuntedBy(this)) return false;
        if (!getProfile().canEatOwnSpecies() && prey.getSpeciesName().equals(this.getSpeciesName())) return false;

        // Kẻ săn mồi và con mồi phải cùng môi trường sống (Cạn - Cạn, Nước - Nước)
        if (this.getProfile().isAquatic() != prey.getProfile().isAquatic()) return false;

        if (prey.getEntityLevel() >= this.getEntityLevel()) return false;
        return prey.getSize() <= this.getSize() * getProfile().getMaxPreySizeMultiplier();
    }

    public boolean canBeHuntedBy(Animal predator) {
        return predator != null;
    }

    public boolean isThreatenedBy(Animal other) {
        if (!getProfile().canBeScared()) return false;
        if (other == null || other == this || !other.isAlive()) return false;
        if (getProfile().isAquatic() != other.getProfile().isAquatic()) return false;
        return other.getEntityLevel() > this.getEntityLevel();
    }

    public boolean canUseStrategy(Class<?> strategyType) {
        if (strategyType == null) return false;
        if (strategyType == model.strategies.HunterStrategy.class) return getProfile().canHunt();
        if (strategyType == model.strategies.ForageStrategy.class) return true; // Mọi loài đều có thể dùng để đi uống nước.
        if (strategyType == model.strategies.ScaredStrategy.class) return getProfile().canBeScared();
        if (strategyType == model.strategies.FlockingStrategy.class) return getProfile().canFlock();
        return true;
    }

    public boolean canForageForFood() {
        return getProfile().canEatPlants() || (getProfile().canEatMeat() && !getProfile().canHunt());
    }

    /** Ăn thịt (FoodSource chung) — dành cho động vật ăn thịt. */
    public void eatMeat(model.items.FoodSource food) {
        if (vitals != null) vitals.eatMeat(food);
    }
    
    /** Đứng ăn dần Carcass theo thời gian. */
    public void eatCarcass(model.items.Carcass carcass, float deltaTime) {
        if (vitals != null) vitals.eatCarcass(carcass, deltaTime);
    }

    /** Uống nước — hồi đầy điểm khát (phải đứng gần nguồn nước). */
    public void drink() {
        if (vitals != null) vitals.drink();
    }

    /** Sinh sản — lớp con bắt buộc override. */
    public abstract Animal reproduce();
    
    /** Sinh xác — lớp con bắt buộc override trả về loại xác tương ứng. */
    protected abstract model.items.Carcass createCarcass();

    /** Tăng tuổi — chết già khi vượt getMaxAge(). */
    public void growOlder(double deltaTime) {
        if (vitals != null) vitals.growOlder(deltaTime);
    }

    /** Kết thúc vòng đời — rớt thịt và xương. */
    public void die(String reason) {
        if (!alive) return;
        setStrategy(null);
        this.alive = false;
        this.isAlive = false;

        exitBush();

        model.world.World currentWorld = getCurrentWorld();
        if (currentWorld != null && this.position != null) {
            model.items.Carcass carcass = createCarcass();
            if (carcass != null) {
                carcass.setWorld(currentWorld); // Gắn world để Carcass rớt xương khi phân hủy
                currentWorld.addEntity(carcass);
            }
            currentWorld.publishEvent(WorldEventType.ANIMAL_DIED, this, reason);
        }
    }

    // =========================================================
    // HỖ TRỢ
    // =========================================================

    public void takeDamage(double amount) {
        if (vitals != null) vitals.takeDamage(amount);
    }

    public float getDamageBlinkTimer() {
        return damageBlinkTimer;
    }

    public void heal(double amount) {
        if (vitals != null) vitals.heal(amount);
    }

    /** Kiểm tra con vật có đứng gần nguồn nước không. */
    public boolean isNearWater() {
        if (world == null) return false;
        float checkDist = this.getSize() / 2 + 15.0f; // Tăng vùng phát hiện nước
        return world.isPositionInWater(position.x + checkDist, position.y) ||
               world.isPositionInWater(position.x - checkDist, position.y) ||
               world.isPositionInWater(position.x, position.y + checkDist) ||
               world.isPositionInWater(position.x, position.y - checkDist);
    }
    public boolean isMoving() { return state != null && state.isMoving(); }
    public void setMoving(boolean moving) { if (state != null) state.setMoving(moving); }
    /** Kiểm tra điều kiện đủ để sinh sản. */
    public boolean canReproduce() {
        return reproduction != null && reproduction.canReproduce();
    }
    public void startReproductionCooldown() {
        if (reproduction != null) reproduction.startReproductionCooldown();
    }
    public boolean canMateWith(Animal other) {
        return reproduction != null && reproduction.canMateWith(other);
    }
    public boolean isHidden() {
        return state != null && state.isHidden();
    }
    public void setHidden(boolean hidden) {
        if (state != null) state.setHidden(hidden);
    }
    public String getActionState() {
        return state != null ? state.getActionState() : "idle";
    }
    public void setActionState(String actionState) {
        if (state != null) state.setActionState(actionState);
    }

    @Override
    public void setSpeed(float speed) {
        super.setSpeed(Math.max(0.0f, speed));
        if (state != null && !state.isSpecialAnimationState(state.getActionState())) {
            state.setActionState(state.resolveLocomotionState());
        }
    }
    public String getAnimationState() {
        return state != null ? state.getAnimationState() : "idle";
    }
    public void markFoodUnsafe(model.entity.Entity food, float duration) {
        if (sensory != null) sensory.markFoodUnsafe(food, duration);
    }
    public boolean isFoodMarkedUnsafe(model.entity.Entity food) {
        return sensory != null && sensory.isFoodMarkedUnsafe(food);
    }

    public void hideInBush(model.structures.Bush bush) {
        if (state != null) {
            state.setHidden(true);
            state.setHiddenInBush(bush);
            state.setMoving(false);
        }
        this.setSpeed(0);
        this.setActionState("idle");
        this.currentVelocity.set(0, 0);
        stuckDetector.reset(); // Reset escape state khi ẩn vào bụi
        if (bush != null) {
            bush.setOccupied(true);
            this.setPosition(bush.getPosition());
        }
    }

    public void exitBush() {
        if (state != null) {
            state.setHidden(false);
            if (state.getHiddenInBush() != null) {
                state.getHiddenInBush().setOccupied(false);
                state.setHiddenInBush(null);
            }
        }
        this.setSpeed(this.baseSpeed);
    }
    public model.structures.Bush getHiddenInBush() {
        return state != null ? state.getHiddenInBush() : null;
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }

    public String getSpriteKey() {
        return getClass().getSimpleName().toLowerCase();
    }

    public boolean isSpriteFacingRightByDefault() {
        return false;
    }

    @Override
    public int getEntityLevel() {
        return getProfile().getEntityLevel();
    }

    // =========================================================
    // toString
    // =========================================================

    public AnimalProfile getProfile() {
        if (profile == null) {
            profile = AnimalProfile.defaultFor(dietType);
        }
        return profile;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[id=%s | hp=%.1f/%.1f | hunger=%.1f/%.1f | thirst=%.1f/%.1f | getAge()=%.1f/%.1f | isAdult()=%b | alive=%b]",
                speciesName, getId(), getHealth(), getMaxHealth(), getHunger(), getMaxHunger(), getThirst(), getMaxThirst(), getAge(), getMaxAge(), isAdult(), alive
        );
    }


    public double getHealth() { return vitals != null ? vitals.getHealth() : 0; }
    public void setHealth(double health) { if (vitals != null) vitals.setHealth(health); }
    public double getMaxHealth() { return vitals != null ? vitals.getMaxHealth() : 1; }
    public void setMaxHealth(double maxHealth) { if (vitals != null) vitals.setMaxHealth(maxHealth); }

    public double getHunger() { return vitals != null ? vitals.getHunger() : 0; }
    public void setHunger(double hunger) { if (vitals != null) vitals.setHunger(hunger); }
    public double getMaxHunger() { return vitals != null ? vitals.getMaxHunger() : 1; }
    public void setMaxHunger(double maxHunger) { if (vitals != null) vitals.setMaxHunger(maxHunger); }
    public double getHungerDecayRate() { return vitals != null ? vitals.getHungerDecayRate() : 0; }
    public void setHungerDecayRate(double hungerDecayRate) { if (vitals != null) vitals.setHungerDecayRate(hungerDecayRate); }

    public double getThirst() { return vitals != null ? vitals.getThirst() : 0; }
    public void setThirst(double thirst) { if (vitals != null) vitals.setThirst(thirst); }
    public double getMaxThirst() { return vitals != null ? vitals.getMaxThirst() : 1; }
    public void setMaxThirst(double maxThirst) { if (vitals != null) vitals.setMaxThirst(maxThirst); }
    public double getThirstDecayRate() { return vitals != null ? vitals.getThirstDecayRate() : 0; }
    public void setThirstDecayRate(double thirstDecayRate) { if (vitals != null) vitals.setThirstDecayRate(thirstDecayRate); }

    public double getAge() { return vitals != null ? vitals.getAge() : 0; }
    public void setAge(double age) { if (vitals != null) vitals.setAge(age); }
    public double getMaxAge() { return vitals != null ? vitals.getMaxAge() : 1; }
    public void setMaxAge(double maxAge) { if (vitals != null) vitals.setMaxAge(maxAge); }

    public boolean isAdult() { return vitals != null && vitals.isAdult(); }
    public void setAdult(boolean adult) { if (vitals != null) vitals.setAdult(adult); }

    public double getVisionRange() { return sensory != null ? sensory.getVisionRange() : 100; }
    public void setVisionRange(double visionRange) { this.sensory.setVisionRange(visionRange); }

    public DietType getDietType() { return dietType; }
    public void setDietType(DietType dietType) { this.dietType = dietType; }
    public model.world.World getWorldRef() { return worldRef; }

    public float getReproductionCooldown() { return reproduction != null ? reproduction.getReproductionCooldown() : 0.0f; }
    public void setReproductionCooldown(float cd) { if (reproduction != null) reproduction.setReproductionCooldown(cd); }
}

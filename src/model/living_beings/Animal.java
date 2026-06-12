package model.living_beings;

import core.Vector2;
import model.items.Carcass;
import model.items.FoodSource;
import model.plants.Plant;
import model.strategies.StrategySelector;
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

    // Sức khỏe, Đói, Khát, Tuổi
    protected AnimalBiology biology = new AnimalBiology();
    protected AnimalMemory memory = new AnimalMemory();

    // Giác quan & trạng thái
    protected double visionRange;
    protected boolean alive;
    protected DietType dietType;
    protected boolean isMoving;
    protected boolean hidden = false;
    protected model.structures.Bush hiddenInBush = null;
    protected String actionState = "idle";
    protected AnimalProfile profile;
    private float reproductionCooldown = 0.0f;

    // Caching for threats
    protected float radarCooldown = 0f;
    protected boolean cachedThreat = false;
    protected float gardenThreatCooldown = 0f;
    protected boolean cachedGardenThreat = false;

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
        biology.setMaxHealth(maxHealth);
        biology.setHealth(maxHealth);
        biology.setMaxHunger(maxHunger);
        biology.setHunger(maxHunger);
        biology.setHungerDecayRate(hungerDecayRate);
        biology.setMaxThirst(maxThirst);
        biology.setThirst(maxThirst);
        biology.setThirstDecayRate(thirstDecayRate);
        biology.setAge(0.0);
        biology.setMaxAge(maxAge);
        this.visionRange = visionRange;
        this.dietType = dietType;
        biology.setAdult(false);
        this.alive = true;
        this.profile = AnimalProfile.defaultFor(dietType);
    }

    /** Constructor rút gọn — lớp con tự gán chỉ số sau khi gọi super(). */
    protected Animal(Vector2 position, float size, float baseSpeed, DietType dietType) {
        super(position, size, baseSpeed);
        this.dietType = dietType;
        biology.setAge(0.0);
        this.alive = true;
        this.profile = AnimalProfile.defaultFor(dietType);
    }

    // =========================================================
    // VÒNG ĐỜI — update()
    // =========================================================

    @Override
    public void update(float deltaTime) {
        // Cập nhật trí nhớ / vùng nguy hiểm
        memory.updateDangerZones(deltaTime);
        memory.updateUnsafeFoodMemory(deltaTime);

        if (reproductionCooldown > 0) {
            reproductionCooldown -= deltaTime;
        }

        growOlder(deltaTime);

        biology.decreaseDamageBlinkTimer(deltaTime);
        boolean isNight = (worldRef != null) && (worldRef.getTimeOfDay() >= 18.0f || worldRef.getTimeOfDay() <= 5.0f);
        boolean isFish = this.getProfile() != null && this.getProfile().isAquatic();

        // --- STUCK DETECTOR ---
        stuckDetector.update(this, deltaTime);
        // Nếu đang trong chế độ thoát khẩn cấp → bỏ qua strategy thường
        if (stuckDetector.isEscaping()) {
            stuckDetector.doEmergencyEscape(this, deltaTime);
            // Vẫn cập nhật sinh học bình thường
            biology.updateNeeds(deltaTime, 1.5f);
            growOlder(deltaTime);
            biology.setAdult(biology.getAge() >= biology.getMaxAge() * 0.2);
            if (biology.getHunger() <= 0) biology.takeDamage(5.0f * deltaTime);
            if (biology.getThirst() <= 0) biology.takeDamage(10.0f * deltaTime);
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
        this.isMoving = distSq > 0.0001f;
        float decayMultiplier = this.isMoving ? 1.5f : 1.0f;

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
        biology.updateNeeds(deltaTime, decayMultiplier);

        biology.setAdult(biology.getAge() >= biology.getMaxAge() * 0.2);

        // Hậu quả đói/khát
        if (biology.getHunger() <= 0) biology.takeDamage(5.0f * deltaTime);
        if (biology.getThirst() <= 0) biology.takeDamage(10.0f * deltaTime);
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


    /**
     * Phát hiện kẻ thù nguy hiểm trong tầm nhìn.
     * Chỉ có ý nghĩa với thú ăn cỏ (HERBIVORE).
     * Thú ăn thịt không cần bỏ chạy.
     */
    protected boolean detectDangerousThreats() {
        if (!getProfile().canBeScared()) return false;
        
        if (radarCooldown > 0) {
            return cachedThreat;
        }
        radarCooldown = 0.5f; // Quét 0.5s 1 lần

        if (worldRef == null || worldRef.getSpatialGrid() == null) return false;

        java.util.List<model.entity.Entity> neighbors =
            worldRef.getSpatialGrid().getNeighbors(this.position, (float) this.visionRange);

        for (model.entity.Entity e : neighbors) {
            if (!(e instanceof Animal) || e == this || !e.isAlive()) continue;
            Animal other = (Animal) e;
            if (isThreatenedBy(other)) {
                model.strategies.IStrategy otherStrategy = other.getCurrentStrategy();
                if (otherStrategy instanceof model.strategies.SleepStrategy) {
                    continue; // Đang ngủ -> Bỏ qua
                }
                boolean isHunting = otherStrategy instanceof model.strategies.HunterStrategy;
                float distSq = this.position.distanceSquared(other.getPosition());
                float maxDist = this instanceof Human
                        ? core.GameConfig.getInstance().THREAT_RADIUS
                        : isHunting ? (float) this.visionRange : (float) this.visionRange * 0.5f;
                
                if (distSq <= maxDist * maxDist) {
                    cachedThreat = true;
                    return true;
                }
            }
        }
        cachedThreat = false;
        return false;
    }

    public boolean hasDangerousThreats() {
        return detectDangerousThreats();
    }

    public boolean hasGardenThreat() {
        if (!getProfile().avoidsGuardedGardens()) return false;
        if (gardenThreatCooldown > 0) {
            return cachedGardenThreat;
        }
        gardenThreatCooldown = 0.5f;
        cachedGardenThreat = false;

        if (worldRef == null) return false;
        cachedGardenThreat = worldRef.getCropManager()
                .isGuardedGardenNear(worldRef, position, (float) visionRange);
        return cachedGardenThreat;
    }
    // =========================================================
    // VÙNG NGUY HIỂM (DANGER ZONE MEMORY)
    // =========================================================
    public void markDangerZone(Entity predator, float radius, float duration) {
        memory.markDangerZone(predator, radius, duration);
    }

    public boolean isInDangerZone(Vector2 pos) {
        return memory.isInDangerZone(pos);
    }

    // =========================================================
    // HÀNH VI
    // =========================================================

    @Override
    public void move(Vector2 direction, float deltaTime) {
        if (!alive || hidden) return;
        super.move(direction, deltaTime);
    }

    /** Ăn thực vật — hồi điểm đói. */
    public void eat(Plant food) {
        if (!alive || food == null || !food.isAlive()) return;
        if (!canEatPlant(food)) return;
        biology.setHunger(biology.getHunger() + food.getNutritionValue());
        food.setAlive(false);
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
        if (!alive || prey == null || prey == this || !prey.isAliveState()) return false;
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
        if (other == null || other == this || !other.isAliveState()) return false;
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
        if (!alive || food == null || !food.isAlive()) return;
        float nutrition = food.consume((float) biology.getMaxHunger()); // Dùng cho Meat cũ (tiêu thụ 1 lần)
        biology.setHunger(biology.getHunger() + nutrition);
    }
    
    /** Đứng ăn dần Carcass theo thời gian. */
    public void eatCarcass(model.items.Carcass carcass, float deltaTime) {
        if (!alive || carcass == null || !carcass.isAlive()) return;
        // Tốc độ cắn: ví dụ 15 khối lượng mỗi giây
        float biteSize = 15.0f * deltaTime;
        float actualNutrition = carcass.consume(biteSize);
        biology.setHunger(biology.getHunger() + actualNutrition);
    }

    /** Uống nước — hồi đầy điểm khát (phải đứng gần nguồn nước). */
    public void drink() {
        if (!alive) return;
        if (isNearWater()) {
            biology.setThirst(biology.getMaxThirst());
        }
    }

    /** Sinh sản — lớp con bắt buộc override. */
    public abstract Animal reproduce();
    
    /** Sinh xác — lớp con bắt buộc override trả về loại xác tương ứng. */
    protected abstract model.items.Carcass createCarcass();

    /** Tăng tuổi — chết già khi vượt maxAge. */
    public void growOlder(double deltaTime) {
        biology.growOlder(deltaTime);
        if (biology.getAge() >= biology.getMaxAge()) {
            die("Già");
        }
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
        if (!alive) return;
        biology.takeDamage(amount);
        if (biology.getHealth() <= 0) die("Kiệt sức (hết máu)");
    }

    public float getDamageBlinkTimer() {
        return biology.getDamageBlinkTimer();
    }

    public void heal(double amount) {
        if (!alive) return;
        biology.heal(amount);
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

    public boolean isMoving() { return isMoving; }

    /** Kiểm tra điều kiện đủ để sinh sản. */
    public boolean canReproduce() {
        if (!alive || !biology.isAdult()) return false;
        if (reproductionCooldown > 0.0f) return false;
        if (biology.getAge() < biology.getMaxAge() * 0.2 || biology.getAge() > biology.getMaxAge() * 0.8) return false;
        if (biology.getHunger() < biology.getMaxHunger() * 0.7 || biology.getThirst() < biology.getMaxThirst() * 0.7) return false;

        // Giới hạn dân số toàn map
        if (worldRef != null && worldRef.getAnimalCount() >= core.GameConfig.getInstance().MAX_ANIMAL_POPULATION) return false;

        return true;
    }

    public void startReproductionCooldown() {
        reproductionCooldown = core.GameConfig.getInstance().REPRODUCTION_COOLDOWN_SECONDS;
    }

    public boolean canMateWith(Animal other) {
        return other != null
                && other != this
                && other.isAliveState()
                && other.canReproduce()
                && other.getSpeciesName().equals(this.getSpeciesName());
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getActionState() {
        return actionState;
    }

    public void setActionState(String actionState) {
        String requestedState = actionState == null ? "idle" : actionState.toLowerCase();
        this.actionState = isSpecialAnimationState(requestedState)
                ? requestedState
                : resolveLocomotionState();
    }

    @Override
    public void setSpeed(float speed) {
        super.setSpeed(Math.max(0.0f, speed));
        if (!isSpecialAnimationState(this.actionState)) {
            this.actionState = resolveLocomotionState();
        }
    }

    public String getAnimationState() {
        if (isSpecialAnimationState(actionState)) {
            return actionState;
        }
        return resolveLocomotionState();
    }

    private String resolveLocomotionState() {
        core.GameConfig config = core.GameConfig.getInstance();
        if (speed <= config.MOVEMENT_SPEED_EPSILON) {
            return "idle";
        }
        return speed > baseSpeed * config.RUN_ANIMATION_SPEED_MULTIPLIER
                ? "run"
                : "walk";
    }

    private boolean isSpecialAnimationState(String state) {
        return "attack".equals(state)
                || "eat".equals(state)
                || "drink".equals(state)
                || "sleep".equals(state);
    }

    public void markFoodUnsafe(model.entity.Entity food, float duration) {
        memory.rememberUnsafeFood(food, duration);
    }

    public boolean isFoodMarkedUnsafe(model.entity.Entity food) {
        return memory.isFoodUnsafe(food);
    }

    public void hideInBush(model.structures.Bush bush) {
        this.hidden = true;
        this.hiddenInBush = bush;
        this.isMoving = false;
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
        this.hidden = false;
        if (this.hiddenInBush != null) {
            this.hiddenInBush.setOccupied(false);
            this.hiddenInBush = null;
        }
        this.setSpeed(this.baseSpeed);
    }

    public model.structures.Bush getHiddenInBush() {
        return this.hiddenInBush;
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }

    public double getHealth() { return biology.getHealth(); }
    public void setHealth(double health) {
        biology.setHealth(health);
        if (biology.getHealth() <= 0) die("Hết máu");
    }
    public double getMaxHealth() { return biology.getMaxHealth(); }
    public void setMaxHealth(double maxHealth) { biology.setMaxHealth(maxHealth); }

    public double getHunger() { return biology.getHunger(); }
    public void setHunger(double hunger) { biology.setHunger(hunger); }
    public double getMaxHunger() { return biology.getMaxHunger(); }
    public void setMaxHunger(double maxHunger) { biology.setMaxHunger(maxHunger); }
    public double getHungerDecayRate() { return biology.getHungerDecayRate(); }
    public void setHungerDecayRate(double hungerDecayRate) { biology.setHungerDecayRate(hungerDecayRate); }

    public double getThirst() { return biology.getThirst(); }
    public void setThirst(double thirst) { biology.setThirst(thirst); }
    public double getMaxThirst() { return biology.getMaxThirst(); }
    public void setMaxThirst(double maxThirst) { biology.setMaxThirst(maxThirst); }
    public double getThirstDecayRate() { return biology.getThirstDecayRate(); }
    public void setThirstDecayRate(double thirstDecayRate) { biology.setThirstDecayRate(thirstDecayRate); }

    public double getAge() { return biology.getAge(); }
    public void setAge(double age) { biology.setAge(age); }
    public double getMaxAge() { return biology.getMaxAge(); }
    public void setMaxAge(double maxAge) { biology.setMaxAge(maxAge); }

    public double getVisionRange() { return visionRange; }
    public void setVisionRange(double visionRange) { this.visionRange = visionRange; }

    public boolean isAdult() { return biology.isAdult(); }
    public void setAdult(boolean adult) { biology.setAdult(adult); }
    public boolean isAliveState() { return alive; }

    public DietType getDietType() { return dietType; }
    public void setDietType(DietType dietType) { this.dietType = dietType; }

    public AnimalProfile getProfile() {
        if (profile == null) {
            profile = AnimalProfile.defaultFor(dietType);
        }
        return profile;
    }

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

    @Override
    public String toString() {
        return String.format(
                "%s[id=%s | hp=%.1f/%.1f | hunger=%.1f/%.1f | thirst=%.1f/%.1f | age=%.1f/%.1f | adult=%b | alive=%b]",
                speciesName, id, biology.getHealth(), biology.getMaxHealth(), biology.getHunger(), biology.getMaxHunger(), biology.getThirst(), biology.getMaxThirst(), biology.getAge(), biology.getMaxAge(), biology.isAdult(), alive
        );
    }
}

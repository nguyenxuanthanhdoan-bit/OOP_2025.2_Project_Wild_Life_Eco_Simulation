package model.living_beings;

import core.Vector2;
import model.plants.Plant;

/**
 * Lớp cơ sở trừu tượng cho mọi loài động vật trong hệ sinh thái.
 * Cây kế thừa: Entity → LivingBeing → Animal → (HerbivoreAnimal / CarnivoreAnimal)
 */
public abstract class Animal extends LivingBeing {

    // =========================================================
    // HẰNG SỐ NGƯỠNG AI
    // =========================================================

    public static final double HUNGER_WARNING_THRESHOLD = 0.50;
    public static final double THIRST_WARNING_THRESHOLD = 0.50;
    public static final double CRITICAL_SURVIVAL_THRESHOLD = 0.15;

    // =========================================================
    // THUỘC TÍNH
    // =========================================================

    protected String speciesName;

    // Sức khỏe
    protected double health;
    protected double maxHealth;

    // Đói
    protected double hunger;
    protected double maxHunger;
    protected double hungerDecayRate;

    // Khát
    protected double thirst;
    protected double maxThirst;
    protected double thirstDecayRate;

    // Tuổi
    protected double age;
    protected double maxAge;

    // Giác quan & trạng thái
    protected double visionRange;
    protected boolean adult;
    protected boolean alive;
    protected DietType dietType;
    protected boolean isMoving;
    protected boolean hidden = false;
    protected model.structures.Bush hiddenInBush = null;
    protected String actionState = "idle";

    /** Tham chiếu tới World hiện tại — được World.update() gán trước mỗi frame. */
    protected model.world.World worldRef = null;
    public void setWorldRef(model.world.World w) { this.worldRef = w; }

    // =========================================================
    // STUCK DETECTOR — Phát hiện và thoát khẩn cấp khi bị kẹt
    // =========================================================
    private Vector2 stuckCheckPos = null;    // Vị trí được lưu lần check trước
    private float stuckTimer = 0f;           // Thời gian đã đứng yên
    private float escapeTimer = 0f;          // Thời gian còn lại của chế độ thoát
    private Vector2 escapeDir = null;        // Hướng thoát đang sử dụng
    private static final float STUCK_THRESHOLD_TIME = 0.4f;  // Giây bất động trước khi bị coi là kẹt
    private static final float STUCK_MOVE_THRESHOLD = 3.0f;  // Pixel/frame tối thiểu để không bị coi là kẹt
    private static final float ESCAPE_DURATION = 0.5f;       // Giây kích hoạt thoát khẩn cấp

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
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.maxHunger = maxHunger;
        this.hunger = maxHunger;
        this.hungerDecayRate = hungerDecayRate;
        this.maxThirst = maxThirst;
        this.thirst = maxThirst;
        this.thirstDecayRate = thirstDecayRate;
        this.age = 0.0;
        this.maxAge = maxAge;
        this.visionRange = visionRange;
        this.dietType = dietType;
        this.adult = false;
        this.alive = true;
    }

    /** Constructor rút gọn — lớp con tự gán chỉ số sau khi gọi super(). */
    protected Animal(Vector2 position, float size, float baseSpeed, DietType dietType) {
        super(position, size, baseSpeed);
        this.dietType = dietType;
        this.age = 0.0;
        this.alive = true;
    }

    // =========================================================
    // VÒNG ĐỜI — update()
    // =========================================================

    @Override
    public void update(float deltaTime) {
        if (!alive) return;

        // --- STUCK DETECTOR ---
        updateStuckDetector(deltaTime);
        // Nếu đang trong chế độ thoát khẩn cấp → bỏ qua strategy thường
        if (escapeTimer > 0) {
            doEmergencyEscape(deltaTime);
            // Vẫn cập nhật sinh học bình thường
            this.hunger = Math.max(0, this.hunger - (this.hungerDecayRate * 1.5f * deltaTime * 0.25f));
            this.thirst = Math.max(0, this.thirst - (this.thirstDecayRate * 1.5f * deltaTime * 0.25f));
            growOlder(deltaTime);
            this.adult = (this.age >= this.maxAge * 0.2);
            if (this.hunger <= 0) takeDamage(5.0f * deltaTime);
            if (this.thirst <= 0) takeDamage(10.0f * deltaTime);
            return;
        }

        decideActiveStrategy();

        Vector2 oldPos = this.position.copy();
        super.update(deltaTime);

        // Tính hệ số tiêu hao (di chuyển tiêu hao nhiều hơn)
        float distSq = this.position.distanceSquared(oldPos);
        this.isMoving = distSq > 0.0001f;
        float decayMultiplier = this.isMoving ? 1.5f : 1.0f;

        // Suy giảm sinh học (× 0.25 để thanh đói/khát tụt chậm hơn 4 lần)
        this.hunger = Math.max(0, this.hunger - (this.hungerDecayRate * decayMultiplier * deltaTime * 0.25f));
        this.thirst = Math.max(0, this.thirst - (this.thirstDecayRate * decayMultiplier * deltaTime * 0.25f));

        growOlder(deltaTime);

        this.adult = (this.age >= this.maxAge * 0.2);

        // Hậu quả đói/khát
        if (this.hunger <= 0) takeDamage(5.0f * deltaTime);
        if (this.thirst <= 0) takeDamage(10.0f * deltaTime);
    }

    /**
     * Cập nhật bộ phát hiện kẹt.
     * Nếu con vật cố di chuyển (speed > 0) nhưng không nhúc nhích trong STUCK_THRESHOLD_TIME giây
     * → kích hoạt thoát khẩn cấp.
     */
    private void updateStuckDetector(float deltaTime) {
        // Chỉ kiểm tra khi đang có ý định di chuyển (speed > 0) và không đang thoát
        if (this.speed <= 0 || escapeTimer > 0) {
            stuckTimer = 0;
            stuckCheckPos = this.position.copy();
            return;
        }

        if (stuckCheckPos == null) {
            stuckCheckPos = this.position.copy();
            return;
        }

        float dist = this.position.distanceTo(stuckCheckPos);
        if (dist < STUCK_MOVE_THRESHOLD) {
            stuckTimer += deltaTime;
            if (stuckTimer >= STUCK_THRESHOLD_TIME) {
                // BỊ KẸT — kích hoạt thoát khẩn cấp
                escapeDir = findBestEscapeDirection();
                escapeTimer = ESCAPE_DURATION;
                stuckTimer = 0;
            }
        } else {
            // Đang di chuyển tốt — reset
            stuckTimer = 0;
            stuckCheckPos = this.position.copy();
        }
    }

    /**
     * Quét 8 hướng xung quanh, chọn hướng có nhiều không gian trống nhất.
     */
    private Vector2 findBestEscapeDirection() {
        model.world.World w = getCurrentWorld();
        Vector2 bestDir = new Vector2(1, 0); // mặc định sang phải
        float maxClearDist = -1f;
        int numDirections = 8;

        for (int i = 0; i < numDirections; i++) {
            double angle = 2 * Math.PI * i / numDirections;
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            Vector2 dir = new Vector2(dx, dy);

            // Thăm dò khoảng trống theo hướng này (probe 40px)
            float probeLen = 40f;
            float clearDist = probeLen;

            if (w != null && w.getSpatialGrid() != null) {
                Vector2 probeEnd = new Vector2(
                    this.position.x + dx * probeLen,
                    this.position.y + dy * probeLen
                );
                java.util.List<model.entity.Entity> nearby =
                    w.getSpatialGrid().getNeighbors(probeEnd, this.size);
                for (model.entity.Entity e : nearby) {
                    if (e != this && e.isSolid() && e.isAlive()) {
                        float d = this.position.distanceTo(e.getPosition()) - this.size / 2 - e.getSize() / 2;
                        if (d < clearDist) clearDist = d;
                    }
                }
            }

            if (clearDist > maxClearDist) {
                maxClearDist = clearDist;
                bestDir = dir;
            }
        }
        return bestDir;
    }

    /** Thực hiện di chuyển thoát khẩn cấp. */
    private void doEmergencyEscape(float deltaTime) {
        escapeTimer -= deltaTime;
        if (escapeDir != null) {
            setActionState("run");
            setSpeed(baseSpeed * 1.5f);
            if (escapeDir.x > 0) setFacingRight(true);
            else if (escapeDir.x < 0) setFacingRight(false);
            super.move(escapeDir, deltaTime);
            isMoving = true;
        }
        if (escapeTimer <= 0) {
            escapeDir = null;
            stuckCheckPos = this.position.copy();
        }
    }

    /** Lấy World hiện tại — override ở subclass nếu cần (mặc định null-safe). */
    protected model.world.World getCurrentWorld() {
        return worldRef; // Được World.update() gán trước mỗi frame
    }

    /**
     * Bộ não AI (Priority Selector): chọn Strategy phù hợp dựa trên trạng thái.
     */
    protected void decideActiveStrategy() {
        // HunterStrategy và ScaredStrategy tự quản lý vòng đời của chúng
        if (currentStrategy instanceof model.strategies.ScaredStrategy ||
            currentStrategy instanceof model.strategies.HunterStrategy) {
            return;
        }

        // Ưu tiên 4: Bỏ chạy khỏi kẻ thù
        if (detectDangerousThreats()) {
            if (!(currentStrategy instanceof model.strategies.ScaredStrategy)) {
                setStrategy(new model.strategies.ScaredStrategy());
            }
            return;
        }

        // Ưu tiên 3: Tìm kiếm thức ăn & nước uống (< 50%)
        if (hunger < maxHunger * HUNGER_WARNING_THRESHOLD || thirst < maxThirst * THIRST_WARNING_THRESHOLD) {
            if (!(currentStrategy instanceof model.strategies.ForageStrategy)) {
                setStrategy(new model.strategies.ForageStrategy());
            }
            return;
        }

        // Ưu tiên 2: MatingStrategy
        if (canReproduce()) {
            if (!(currentStrategy instanceof model.strategies.MatingStrategy)) {
                setStrategy(new model.strategies.MatingStrategy());
            }
            return;
        }

        // Ưu tiên 1: FlockingStrategy (TODO)

        // Ưu tiên 0: Mặc định — đi dạo
        if (!(currentStrategy instanceof model.strategies.PassiveStrategy)) {
            setStrategy(new model.strategies.PassiveStrategy());
        }
    }

    /** Stub — người phụ trách nhánh hunter-scared sẽ triển khai. */
    protected boolean detectDangerousThreats() {
        return false;
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
        this.hunger = Math.min(this.maxHunger, this.hunger + food.getNutritionValue());
        food.setAlive(false);
    }

    /** Ăn thịt (FoodSource chung) — dành cho động vật ăn thịt. */
    public void eatMeat(model.items.FoodSource food) {
        if (!alive || food == null || !food.isAlive()) return;
        float nutrition = food.consume((float) this.maxHunger); // Dùng cho Meat cũ (tiêu thụ 1 lần)
        this.hunger = Math.min(this.maxHunger, this.hunger + nutrition);
    }
    
    /** Đứng ăn dần Carcass theo thời gian. */
    public void eatCarcass(model.items.Carcass carcass, float deltaTime) {
        if (!alive || carcass == null || !carcass.isAlive()) return;
        // Tốc độ cắn: ví dụ 15 khối lượng mỗi giây
        float biteSize = 15.0f * deltaTime;
        float actualNutrition = carcass.consume(biteSize);
        this.hunger = Math.min(this.maxHunger, this.hunger + actualNutrition);
    }

    /** Uống nước — hồi đầy điểm khát (phải đứng gần nguồn nước). */
    public void drink() {
        if (!alive) return;
        if (isNearWater()) {
            this.thirst = this.maxThirst;
        }
    }

    /** Sinh sản — lớp con bắt buộc override. */
    public abstract Animal reproduce();
    
    /** Sinh xác — lớp con bắt buộc override trả về loại xác tương ứng. */
    protected abstract model.items.Carcass createCarcass();

    /** Tăng tuổi — chết già khi vượt maxAge. */
    public void growOlder(double deltaTime) {
        this.age += deltaTime;
        if (this.age >= this.maxAge) {
            die("Già");
        }
    }

    /** Kết thúc vòng đời — rớt thịt và xương. */
    public void die(String reason) {
        if (!alive) return;
        this.alive = false;
        this.isAlive = false;

        exitBush();

        if (world != null && this.position != null) {
            model.items.Carcass carcass = createCarcass();
            carcass.setWorld(world); // Gắn world để Carcass rớt xương khi phân hủy
            world.addEntity(carcass);
            
            model.world.PopulationManager.onAnimalDeath(this, world);
        }
    }

    // =========================================================
    // HỖ TRỢ
    // =========================================================

    public void takeDamage(double amount) {
        if (!alive) return;
        this.health = Math.max(0, this.health - amount);
        if (this.health <= 0) die("Kiệt sức (hết máu)");
    }

    public void heal(double amount) {
        if (!alive) return;
        this.health = Math.min(this.maxHealth, this.health + amount);
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
        if (!alive || !adult) return false;
        if (age < maxAge * 0.2 || age > maxAge * 0.8) return false;
        if (hunger < maxHunger * 0.7 || thirst < maxThirst * 0.7) return false;
        return true;
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
        this.actionState = actionState;
    }

    public void hideInBush(model.structures.Bush bush) {
        this.hidden = true;
        this.hiddenInBush = bush;
        if (bush != null) {
            bush.setOccupied(true);
            this.setPosition(bush.getPosition());
        }
        this.setSpeed(0);
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

    public double getHealth() { return health; }
    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
        if (this.health <= 0) die("Hết máu");
    }
    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    public double getHunger() { return hunger; }
    public void setHunger(double hunger) { this.hunger = Math.max(0, Math.min(maxHunger, hunger)); }
    public double getMaxHunger() { return maxHunger; }
    public void setMaxHunger(double maxHunger) { this.maxHunger = maxHunger; }
    public double getHungerDecayRate() { return hungerDecayRate; }
    public void setHungerDecayRate(double hungerDecayRate) { this.hungerDecayRate = hungerDecayRate; }

    public double getThirst() { return thirst; }
    public void setThirst(double thirst) { this.thirst = Math.max(0, Math.min(maxThirst, thirst)); }
    public double getMaxThirst() { return maxThirst; }
    public void setMaxThirst(double maxThirst) { this.maxThirst = maxThirst; }
    public double getThirstDecayRate() { return thirstDecayRate; }
    public void setThirstDecayRate(double thirstDecayRate) { this.thirstDecayRate = thirstDecayRate; }

    public double getAge() { return age; }
    public void setAge(double age) { this.age = age; }
    public double getMaxAge() { return maxAge; }
    public void setMaxAge(double maxAge) { this.maxAge = maxAge; }

    public double getVisionRange() { return visionRange; }
    public void setVisionRange(double visionRange) { this.visionRange = visionRange; }

    public boolean isAdult() { return adult; }
    public void setAdult(boolean adult) { this.adult = adult; }
    public boolean isAliveState() { return alive; }

    public DietType getDietType() { return dietType; }
    public void setDietType(DietType dietType) { this.dietType = dietType; }

    // =========================================================
    // toString
    // =========================================================

    @Override
    public String toString() {
        return String.format(
                "%s[id=%s | hp=%.1f/%.1f | hunger=%.1f/%.1f | thirst=%.1f/%.1f | age=%.1f/%.1f | adult=%b | alive=%b]",
                speciesName, id, health, maxHealth, hunger, maxHunger, thirst, maxThirst, age, maxAge, adult, alive
        );
    }
}
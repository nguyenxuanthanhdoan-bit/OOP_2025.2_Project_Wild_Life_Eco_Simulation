package model.living_beings;

import core.Vector2;
import model.entity.Entity;
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
    protected boolean isEating = false;
    protected boolean isDrinking = false;
    protected float actionTimer = 0.0f;
    protected Entity targetFood = null; // Thêm biến lưu thức ăn đang ăn

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

        // Xử lý Ăn (theo thời gian)
        if (isEating) {
            actionTimer += deltaTime;
            if (actionTimer >= 2.0f) {
                isEating = false;
                if (targetFood != null && targetFood.isAlive()) {
                    if (targetFood instanceof Plant) {
                        this.hunger = Math.min(this.maxHunger, this.hunger + ((Plant)targetFood).getNutritionValue());
                    } else if (targetFood instanceof model.items.Meat) {
                        this.hunger = Math.min(this.maxHunger, this.hunger + ((model.items.Meat)targetFood).getNutritionValue());
                    }
                    targetFood.setAlive(false);
                }
                targetFood = null;
            } else {
                return; // Đứng im nhai
            }
        }

        // Xử lý Uống (hồi từ từ cho đến khi đầy)
        if (isDrinking) {
            actionTimer += deltaTime;
            if (this.thirst >= this.maxThirst || !isNearWater()) {
                isDrinking = false;
                this.thirst = Math.min(this.thirst, this.maxThirst);
            } else {
                this.thirst += 30.0 * deltaTime; // Hồi 30 điểm mỗi giây
                return; // Đứng im uống
            }
        }

        decideActiveStrategy();

        Vector2 oldPos = this.position.copy();
        super.update(deltaTime);

        // Tính hệ số tiêu hao (di chuyển tiêu hao nhiều hơn)
        float distSq = this.position.distanceSquared(oldPos);
        this.isMoving = distSq > 0.0001f;
        float decayMultiplier = 1.0f;
        if (this.isMoving) {
            decayMultiplier = (getSpeed() > getBaseSpeed()) ? 3.0f : 1.5f;
        }

        // Suy giảm sinh học (Giảm tốc độ tụt đói/khát đi 4 lần để thanh trạng thái tụt chậm hơn)
        this.hunger = Math.max(0, this.hunger - (this.hungerDecayRate * decayMultiplier * deltaTime * 0.25f));
        this.thirst = Math.max(0, this.thirst - (this.thirstDecayRate * decayMultiplier * deltaTime * 0.25f));

        growOlder(deltaTime);

        this.adult = (this.age >= this.maxAge * 0.2);

        // Hậu quả đói/khát
        if (this.hunger <= 0) takeDamage(5.0f * deltaTime);
        if (this.thirst <= 0) takeDamage(10.0f * deltaTime);
    }

    /**
     * Bộ não AI (Priority Selector): chọn Strategy phù hợp dựa trên trạng thái.
     * Các TODO sẽ được người phụ trách từng nhánh bật lên khi triển khai.
     */
    protected void decideActiveStrategy() {

        // Ưu tiên 4: Bỏ chạy khỏi kẻ thù
        if (detectDangerousThreats()) {
            // TODO: Kích hoạt ScaredStrategy
            // (Nhánh hunter-scared sẽ xử lý)
            return;
        }

        // Ưu tiên 3: Tìm kiếm thức ăn & nước uống (< 50%)
        if (hunger < maxHunger * HUNGER_WARNING_THRESHOLD || thirst < maxThirst * THIRST_WARNING_THRESHOLD) {
            if (!(currentStrategy instanceof model.strategies.ForageStrategy)) {
                setStrategy(new model.strategies.ForageStrategy());
            }
            return;
        }

        // Ưu tiên 2: MatingStrategy (TODO)
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

    public void move(Vector2 direction, float deltaTime) {
        if (!alive) return;
        super.move(direction, deltaTime);
    }

    /** Ăn thực vật — hồi điểm đói. */
    public void eat(Plant food) {
        if (!alive || food == null || !food.isAlive()) return;
        this.targetFood = food;
        this.isEating = true;
        this.actionTimer = 0.0f; // Bắt đầu đếm thời gian nhai từ 0
    }

    /** Ăn thịt — hồi điểm đói (dành cho động vật ăn thịt). */
    public void eatMeat(model.items.Meat meat) {
        if (!alive || meat == null || !meat.isAlive()) return;
        this.targetFood = meat;
        this.isEating = true;
        this.actionTimer = 0.0f; // Bắt đầu đếm thời gian nhai từ 0
    }

    /** Bắt đầu uống nước — hồi từ từ trong hàm update(). */
    public void drink() {
        if (!alive) return;
        if (isNearWater()) {
            this.isDrinking = true;
            this.actionTimer = 0.0f; // Đếm thời gian uống
        }
    }

    /** Sinh sản — lớp con bắt buộc override. */
    public abstract Animal reproduce();

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

        if (world != null && this.position != null) {
            model.items.Meat meat = new model.items.Meat(new core.Vector2(this.position.x - 5, this.position.y));
            model.items.Bone bone = new model.items.Bone(new core.Vector2(this.position.x + 5, this.position.y));
            world.addEntity(meat);
            world.addEntity(bone);
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
        if (world == null || world.getSpatialGrid() == null) return false;
        // Kiểm tra đúng mép chạm nước (tăng lên để không bị kẹt bởi biên va chạm)
        float checkDist = this.getSize() / 2 + 15.0f;
        float diag = checkDist * 0.707f; // sin(45 độ)
        
        return world.isPositionInWater(position.x + checkDist, position.y) ||
               world.isPositionInWater(position.x - checkDist, position.y) ||
               world.isPositionInWater(position.x, position.y + checkDist) ||
               world.isPositionInWater(position.x, position.y - checkDist) ||
               world.isPositionInWater(position.x + diag, position.y + diag) ||
               world.isPositionInWater(position.x - diag, position.y + diag) ||
               world.isPositionInWater(position.x + diag, position.y - diag) ||
               world.isPositionInWater(position.x - diag, position.y - diag);
    }

    public boolean isMoving() { return isMoving; }

    /** Kiểm tra điều kiện đủ để sinh sản. */
    public boolean canReproduce() {
        if (!alive || !adult) return false;
        if (age < maxAge * 0.2 || age > maxAge * 0.8) return false;
        if (hunger < maxHunger * 0.7 || thirst < maxThirst * 0.7) return false;
        return true;
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
    public boolean isEatingState() { return isEating; }
    public boolean isDrinkingState() { return isDrinking; }
    public float getActionTimer() { return actionTimer; }

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
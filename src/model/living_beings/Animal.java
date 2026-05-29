package model.living_beings;

import core.Vector2;
import model.plants.Plant;

/**
 * Lớp cơ sở cho các loài động vật hoang dã.
 * Quản lý toàn bộ chỉ số sinh học: máu, đói, khát, tuổi thọ, tương tác ăn uống, v.v.
 */
public abstract class Animal extends LivingBeing {

    protected String speciesName;

    // Chỉ số máu
    protected float health;
    protected float maxHealth;

    // Chỉ số đói (giảm dần về 0 là đói lả)
    protected float hunger;
    protected float maxHunger;
    protected float hungerDecayRate;

    // Chỉ số khát (giảm dần về 0 là khát khô)
    protected float thirst;
    protected float maxThirst;
    protected float thirstDecayRate;

    // Chỉ số tuổi thọ
    protected float age;
    protected float maxAge;

    // Đặc tính sinh học
    protected DietType dietType;

    public Animal(Vector2 position, float size, float baseSpeed, DietType dietType) {
        super(position, size, baseSpeed);
        this.dietType = dietType;
        this.age = 0.0f;
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive) return;

        // 1. Lưu lại tọa độ CŨ trước khi di chuyển
        Vector2 oldPos = this.position.copy();

        // 2. Chạy logic hành động/di chuyển (ủy quyền cho Strategy)
        super.update(deltaTime);

        // 3. Kiểm tra xem có đang di chuyển không để tính hệ số động năng
        float distSq = this.position.distanceSquared(oldPos);
        boolean isMoving = distSq > 0.0001f;
        float decayMultiplier = isMoving ? 1.5f : 1.0f;

        // 4. Suy giảm các chỉ số sinh học và tăng tuổi thọ
        this.hunger = Math.max(0, this.hunger - (this.hungerDecayRate * decayMultiplier * deltaTime));
        this.thirst = Math.max(0, this.thirst - (this.thirstDecayRate * decayMultiplier * deltaTime));
        this.age += deltaTime;

        // 5. Kiểm tra điều kiện sống còn
        if (this.age >= this.maxAge) {
            die("Chết vì tuổi già");
            return;
        }

        if (this.hunger <= 0) {
            takeDamage(5.0f * deltaTime); // Mất máu mỗi giây khi đói lả
        }
        
        if (this.thirst <= 0) {
            takeDamage(10.0f * deltaTime); // Mất máu mỗi giây khi khát (khát nguy hiểm hơn đói)
        }
    }

    /**
     * Hành vi ăn thức ăn (Thực vật)
     */
    public void eat(Plant food) {
        if (food != null && food.isAlive()) {
            this.hunger = Math.min(this.maxHunger, this.hunger + food.getNutritionValue());
            // Cắn nuốt và xóa sổ thực vật này
            food.setAlive(false);
            System.out.println(this.speciesName + " đã ăn một " + food.getClass().getSimpleName());
        }
    }

    /**
     * Hành vi uống nước
     */
    public void drink() {
        if (isNearWater()) {
            this.thirst = this.maxThirst;
            System.out.println(this.speciesName + " đã uống nước và giải khát hoàn toàn.");
        }
    }

    /**
     * Kiểm tra xem con vật có đang ở gần nguồn nước không
     */
    public boolean isNearWater() {
        if (this.world != null) {
            // Kiểm tra ngay tại vị trí đứng
            return this.world.isPositionInWater(this.position.x, this.position.y);
        }
        return false;
    }

    /**
     * Điều kiện sinh sản khắt khe
     */
    public boolean canReproduce() {
        if (!isAlive) return false;

        // Độ tuổi trưởng thành: từ 20% đến 80% tuổi thọ
        float minBreedingAge = this.maxAge * 0.2f;
        float maxBreedingAge = this.maxAge * 0.8f;
        if (this.age < minBreedingAge || this.age > maxBreedingAge) {
            return false;
        }

        // Thể trạng: đói khát phải từ 70% trở lên
        if (this.hunger < this.maxHunger * 0.7f || this.thirst < this.maxThirst * 0.7f) {
            return false;
        }

        return true;
    }

    public void takeDamage(float amount) {
        this.health -= amount;
        if (this.health <= 0) {
            this.health = 0;
            die("Kiệt sức (hết máu)");
        }
    }

    protected void die(String reason) {
        this.isAlive = false;
        System.out.println(this.speciesName + " (" + this.id + ") đã chết! Nguyên nhân: " + reason);
    }

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================
    public String getSpeciesName() { return speciesName; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getHunger() { return hunger; }
    public float getMaxHunger() { return maxHunger; }
    public float getThirst() { return thirst; }
    public float getMaxThirst() { return maxThirst; }
    public float getAge() { return age; }
    public float getMaxAge() { return maxAge; }
    public DietType getDietType() { return dietType; }
}
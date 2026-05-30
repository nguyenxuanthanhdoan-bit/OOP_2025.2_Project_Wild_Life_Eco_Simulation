package model.living_beings;

import core.Vector2;
import model.plants.Plant;

/**
 * Lớp cơ sở trừu tượng đại diện cho mọi loài động vật trong hệ sinh thái.
 *
 * <p>Kế thừa từ {@link LivingBeing}, Animal bổ sung đầy đủ các chỉ số sinh học:
 * sức khỏe, đói, khát, tầm nhìn, tuổi thọ và trạng thái trưởng thành.
 * Các hành vi cụ thể (di chuyển, ăn, sinh sản, v.v.) được thiết kế theo
 * nguyên tắc OOP — một số để abstract để lớp con tự triển khai chi tiết,
 * một số cung cấp logic chung có thể override.</p>
 *
 * <p><b>Cây kế thừa:</b> Entity → LivingBeing → Animal → (HerbivoreAnimal / CarnivoreAnimal / ...)</p>
 *
 * @author OOP-2025.2 Team
 */
public abstract class Animal extends LivingBeing {

    // =========================================================
    // THUỘC TÍNH ĐỊNH DANH
    // =========================================================

    /** Tên loài (vd: "Thỏ", "Sói", "Hươu") */
    protected String speciesName;

    // =========================================================
    // CHỈ SỐ SỨC KHỎE
    // =========================================================

    /** Điểm sức khỏe hiện tại. Về 0 → chết. */
    protected double health;

    /** Điểm sức khỏe tối đa. */
    protected double maxHealth;

    // =========================================================
    // CHỈ SỐ ĐÓI
    // =========================================================

    /** Chỉ số đói hiện tại (0 = đói lả, maxHunger = no). */
    protected double hunger;

    /** Ngưỡng đói tối đa. */
    protected double maxHunger;

    /**
     * Tốc độ suy giảm đói mỗi giây (đơn vị: điểm/giây).
     * Tăng thêm khi di chuyển (decayMultiplier).
     */
    protected double hungerDecayRate;

    // =========================================================
    // CHỈ SỐ KHÁT
    // =========================================================

    /** Chỉ số khát hiện tại (0 = khát khô, maxThirst = no khát). */
    protected double thirst;

    /** Ngưỡng khát tối đa. */
    protected double maxThirst;

    /**
     * Tốc độ suy giảm khát mỗi giây (đơn vị: điểm/giây).
     * Khát thường nguy hiểm hơn đói → thirstDecayRate nên cao hơn hungerDecayRate.
     */
    protected double thirstDecayRate;

    // =========================================================
    // CHỈ SỐ TUỔI
    // =========================================================

    /** Tuổi hiện tại tính bằng giây mô phỏng. */
    protected double age;

    /** Tuổi thọ tối đa (giây mô phỏng). Vượt quá → chết già. */
    protected double maxAge;

    // =========================================================
    // CHỈ SỐ GIÁC QUAN & DI CHUYỂN
    // =========================================================

    /**
     * Tầm nhìn (pixel / đơn vị thế giới).
     * Dùng để phát hiện con mồi, nguồn nước, kẻ thù, bạn tình.
     */
    protected double visionRange;

    // =========================================================
    // TRẠNG THÁI SINH HỌC
    // =========================================================

    /** True nếu con vật đã đạt độ tuổi trưởng thành (có thể sinh sản). */
    protected boolean adult;

    /**
     * True nếu con vật còn sống.
     * Lưu ý: {@code isAlive} (boolean, kế thừa từ Entity) đã tồn tại;
     * trường {@code alive} này là alias được expose qua getter/setter riêng
     * để tuân thủ đúng yêu cầu đặc tả (boolean alive).
     * Hai trường luôn được giữ đồng bộ qua {@link #die(String)}.
     */
    protected boolean alive;

    /** Chế độ ăn của loài. */
    protected DietType dietType;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Constructor đầy đủ tham số cho Animal.
     *
     * @param position        Vị trí khởi tạo trên bản đồ
     * @param size            Kích thước hình học (bán kính / chiều dài cạnh)
     * @param baseSpeed       Tốc độ di chuyển cơ bản (pixel/giây)
     * @param speciesName     Tên loài
     * @param maxHealth       Sức khỏe tối đa
     * @param maxHunger       Chỉ số đói tối đa
     * @param hungerDecayRate Tốc độ suy giảm đói mỗi giây
     * @param maxThirst       Chỉ số khát tối đa
     * @param thirstDecayRate Tốc độ suy giảm khát mỗi giây
     * @param maxAge          Tuổi thọ tối đa (giây mô phỏng)
     * @param visionRange     Tầm nhìn (đơn vị thế giới)
     * @param dietType        Chế độ ăn (HERBIVORE / CARNIVORE / OMNIVORE)
     */
    public Animal(
            Vector2 position,
            float size,
            float baseSpeed,
            String speciesName,
            double maxHealth,
            double maxHunger,
            double hungerDecayRate,
            double maxThirst,
            double thirstDecayRate,
            double maxAge,
            double visionRange,
            DietType dietType
    ) {
        super(position, size, baseSpeed);

        this.speciesName = speciesName;

        this.maxHealth = maxHealth;
        this.health = maxHealth;          // Sinh ra với sức khỏe đầy đủ

        this.maxHunger = maxHunger;
        this.hunger = maxHunger;          // Sinh ra không đói
        this.hungerDecayRate = hungerDecayRate;

        this.maxThirst = maxThirst;
        this.thirst = maxThirst;          // Sinh ra không khát
        this.thirstDecayRate = thirstDecayRate;

        this.age = 0.0;
        this.maxAge = maxAge;

        this.visionRange = visionRange;
        this.dietType = dietType;

        this.adult = false;               // Bắt đầu là con non
        this.alive = true;                // Sinh ra còn sống
    }

    /**
     * Constructor rút gọn — dành cho các lớp con tự thiết lập chỉ số bên trong constructor.
     * Các lớp con phải tự gán đầy đủ các trường sau khi gọi super().
     *
     * @param position  Vị trí khởi tạo
     * @param size      Kích thước
     * @param baseSpeed Tốc độ cơ bản
     * @param dietType  Chế độ ăn
     */
    protected Animal(Vector2 position, float size, float baseSpeed, DietType dietType) {
        super(position, size, baseSpeed);
        this.dietType = dietType;
        this.age = 0.0;
        this.alive = true;
    }

    // =========================================================
    // VÒNG ĐỜI — update()
    // =========================================================

    /**
     * Cập nhật toàn bộ trạng thái sinh học mỗi frame.
     * Thứ tự thực hiện:
     * <ol>
     *   <li>Di chuyển theo Strategy (ủy quyền cho lớp cha)</li>
     *   <li>Suy giảm đói, khát; tăng tuổi</li>
     *   <li>Kiểm tra trưởng thành ({@link #adult})</li>
     *   <li>Kiểm tra điều kiện chết (đói, khát, già, hết máu)</li>
     * </ol>
     *
     * @param deltaTime Thời gian trôi qua giữa 2 frame (giây)
     */
    @Override
    public void update(float deltaTime) {
        if (!alive) return;

        // --- 1. Ghi lại vị trí cũ để tính hệ số vận động ---
        Vector2 oldPos = this.position.copy();

        // --- 2. Chạy Strategy (bao gồm move) ---
        super.update(deltaTime);

        // --- 3. Tính hệ số tiêu hao dựa trên vận động ---
        float distSq = this.position.distanceSquared(oldPos);
        boolean isMoving = distSq > 0.0001f;
        float decayMultiplier = isMoving ? 1.5f : 1.0f;

        // --- 4. Suy giảm chỉ số sinh học ---
        this.hunger = Math.max(0, this.hunger - (this.hungerDecayRate * decayMultiplier * deltaTime));
        this.thirst = Math.max(0, this.thirst - (this.thirstDecayRate * decayMultiplier * deltaTime));

        // --- 5. Tăng tuổi ---
        growOlder(deltaTime);

        // --- 6. Kiểm tra trưởng thành ---
        double minAdultAge = this.maxAge * 0.2;
        this.adult = (this.age >= minAdultAge);

        // --- 7. Hậu quả đói/khát ---
        if (this.hunger <= 0) {
            takeDamage(5.0f * deltaTime);   // Mất máu do đói lả
        }
        if (this.thirst <= 0) {
            takeDamage(10.0f * deltaTime);  // Mất máu do khát (khát nguy hiểm hơn)
        }
    }

    // =========================================================
    // PHƯƠNG THỨC HÀNH VI
    // =========================================================

    /**
     * Di chuyển con vật theo một hướng cụ thể.
     * Triển khai mặc định ủy quyền cho {@link LivingBeing#move(Vector2, float)}.
     * Lớp con có thể override để thêm logic riêng (vd: Sói chạy nước rút).
     *
     * @param direction Hướng di chuyển (Vector2 đã normalize)
     * @param deltaTime Thời gian trôi qua (giây)
     */
    public void move(Vector2 direction, float deltaTime) {
        if (!alive) return;
        super.move(direction, deltaTime);
    }

    /**
     * Hành vi ăn thực vật. Tăng chỉ số {@code hunger}.
     * Override trong lớp con nếu cần xử lý ăn thịt (CarnivoreAnimal).
     *
     * @param food Cây/thực vật muốn ăn (không null)
     */
    public void eat(Plant food) {
        if (!alive) return;
        if (food != null && food.isAlive()) {
            this.hunger = Math.min(this.maxHunger, this.hunger + food.getNutritionValue());
            food.setAlive(false);
            System.out.printf("[%s] ăn %s → đói: %.1f/%.1f%n",
                    speciesName, food.getClass().getSimpleName(), hunger, maxHunger);
        }
    }

    /**
     * Hành vi uống nước. Khôi phục {@code thirst} về tối đa.
     * Chỉ có hiệu lực khi con vật đứng cạnh nguồn nước ({@link #isNearWater()}).
     */
    public void drink() {
        if (!alive) return;
        if (isNearWater()) {
            this.thirst = this.maxThirst;
            System.out.printf("[%s] uống nước → khát: %.1f/%.1f%n",
                    speciesName, thirst, maxThirst);
        }
    }

    /**
     * Hành vi sinh sản. Trả về offspring mới nếu đủ điều kiện, ngược lại null.
     * Bắt buộc override ở lớp con cụ thể (Rabbit, Wolf, ...) vì mỗi loài
     * có cách khởi tạo con khác nhau.
     *
     * @return Con vật con mới được tạo ra, hoặc {@code null} nếu không sinh sản được
     */
    public abstract Animal reproduce();

    /**
     * Tăng tuổi theo thời gian mô phỏng.
     * Khi vượt quá {@code maxAge}, con vật sẽ tự gọi {@link #die(String)}.
     *
     * @param deltaTime Thời gian trôi qua (giây)
     */
    public void growOlder(double deltaTime) {
        this.age += deltaTime;
        if (this.age >= this.maxAge) {
            die("Già");
        }
    }

    /**
     * Kết thúc vòng đời của con vật.
     * Đặt {@code alive} và {@code isAlive} (kế thừa) về false,
     * đồng thời in thông điệp lý do tử vong.
     *
     * @param reason Nguyên nhân tử vong (mô tả ngắn, vd: "Già", "Bị ăn thịt")
     */
    public void die(String reason) {
        this.alive = false;
        this.isAlive = false;       // Đồng bộ với trường kế thừa từ Entity
        System.out.printf("[%s | %s] đã chết! Nguyên nhân: %s (tuổi: %.1f)%n",
                speciesName, id, reason, age);
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // =========================================================

    /**
     * Trừ sức khỏe (nhận sát thương).
     * Tự động gọi {@link #die(String)} khi health về 0.
     *
     * @param amount Lượng sát thương (dương)
     */
    public void takeDamage(double amount) {
        if (!alive) return;
        this.health = Math.max(0, this.health - amount);
        if (this.health <= 0) {
            die("Kiệt sức (hết máu)");
        }
    }

    /**
     * Hồi phục sức khỏe (không vượt quá maxHealth).
     *
     * @param amount Lượng hồi phục (dương)
     */
    public void heal(double amount) {
        if (!alive) return;
        this.health = Math.min(this.maxHealth, this.health + amount);
    }

    /**
     * Kiểm tra con vật có đứng cạnh nguồn nước không.
     * Tận dụng API của {@link model.world.World}.
     *
     * @return true nếu đang đứng trên ô nước
     */
    public boolean isNearWater() {
        if (this.world != null) {
            return this.world.isPositionInWater(this.position.x, this.position.y);
        }
        return false;
    }

    /**
     * Kiểm tra điều kiện đủ để sinh sản:
     * <ul>
     *   <li>Còn sống</li>
     *   <li>Đã trưởng thành ({@link #adult})</li>
     *   <li>Tuổi nằm trong khoảng sinh sản (20% – 80% maxAge)</li>
     *   <li>Đói/khát đều ≥ 70% tối đa</li>
     * </ul>
     *
     * @return true nếu đủ điều kiện sinh sản
     */
    public boolean canReproduce() {
        if (!alive || !adult) return false;

        double minBreedingAge = maxAge * 0.2;
        double maxBreedingAge = maxAge * 0.8;
        if (age < minBreedingAge || age > maxBreedingAge) return false;

        if (hunger < maxHunger * 0.7 || thirst < maxThirst * 0.7) return false;

        return true;
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    // --- Định danh ---

    public String getSpeciesName() {
        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    // --- Sức khỏe ---

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
        if (this.health <= 0) die("Hết máu (setHealth)");
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    // --- Đói ---

    public double getHunger() {
        return hunger;
    }

    public void setHunger(double hunger) {
        this.hunger = Math.max(0, Math.min(maxHunger, hunger));
    }

    public double getMaxHunger() {
        return maxHunger;
    }

    public void setMaxHunger(double maxHunger) {
        this.maxHunger = maxHunger;
    }

    public double getHungerDecayRate() {
        return hungerDecayRate;
    }

    public void setHungerDecayRate(double hungerDecayRate) {
        this.hungerDecayRate = hungerDecayRate;
    }

    // --- Khát ---

    public double getThirst() {
        return thirst;
    }

    public void setThirst(double thirst) {
        this.thirst = Math.max(0, Math.min(maxThirst, thirst));
    }

    public double getMaxThirst() {
        return maxThirst;
    }

    public void setMaxThirst(double maxThirst) {
        this.maxThirst = maxThirst;
    }

    public double getThirstDecayRate() {
        return thirstDecayRate;
    }

    public void setThirstDecayRate(double thirstDecayRate) {
        this.thirstDecayRate = thirstDecayRate;
    }

    // --- Tuổi ---

    public double getAge() {
        return age;
    }

    public void setAge(double age) {
        this.age = age;
    }

    public double getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(double maxAge) {
        this.maxAge = maxAge;
    }

    // --- Tầm nhìn ---

    public double getVisionRange() {
        return visionRange;
    }

    public void setVisionRange(double visionRange) {
        this.visionRange = visionRange;
    }

    // --- Trạng thái ---

    public boolean isAdult() {
        return adult;
    }

    public void setAdult(boolean adult) {
        this.adult = adult;
    }

    /** Trả về trạng thái sống (alias cho {@code isAlive()} kế thừa từ Entity). */
    public boolean isAliveState() {
        return alive;
    }

    // Note: setAlive(boolean) được kế thừa từ Entity.
    // Sử dụng die() để kết thúc vòng đời một cách có kiểm soát.

    // --- Chế độ ăn ---

    public DietType getDietType() {
        return dietType;
    }

    public void setDietType(DietType dietType) {
        this.dietType = dietType;
    }

    // =========================================================
    // toString — Hỗ trợ debug
    // =========================================================

    @Override
    public String toString() {
        return String.format(
                "%s[id=%s | hp=%.1f/%.1f | hunger=%.1f/%.1f | thirst=%.1f/%.1f" +
                " | age=%.1f/%.1f | adult=%b | alive=%b]",
                speciesName, id,
                health, maxHealth,
                hunger, maxHunger,
                thirst, maxThirst,
                age, maxAge,
                adult, alive
        );
    }
}
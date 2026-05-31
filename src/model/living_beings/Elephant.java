package model.living_beings;

import core.DisplayMode;
import core.Vector2;
import model.entity.Entity;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Plant;
import model.strategies.PassiveStrategy;
import model.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lớp đại diện cho loài <b>Voi</b> trong hệ sinh thái.
 *
 * <h3>Đặc điểm sinh học:</h3>
 * <ul>
 *   <li>Loài ăn cỏ (HERBIVORE) — ăn được cả {@link Grass} lẫn {@link Fruit}.</li>
 *   <li>Kích thước lớn nhất hệ sinh thái — mọi loài phải nhường đường.</li>
 *   <li>Không sợ Wolf, Tiger hay Hunter — <b>không bao giờ flee()</b>.</li>
 *   <li>Không thể bị Wolf hoặc Tiger săn (quá to và nguy hiểm).</li>
 *   <li>Tuổi thọ rất cao.</li>
 *   <li>Chỉ chết vì: tuổi già, đói lả, hoặc khát.</li>
 * </ul>
 *
 * <h3>Hành vi xã hội:</h3>
 * <ul>
 *   <li>Di chuyển theo đàn — mỗi đàn có {@code herdId} riêng.</li>
 *   <li>{@link #leadHerd()} điều phối hướng di chuyển của đàn.</li>
 * </ul>
 *
 * <h3>Sinh sản:</h3>
 * <ul>
 *   <li>Yêu cầu {@code adult == true}.</li>
 *   <li>Yêu cầu sức khỏe ≥ {@value #MIN_HEALTH_TO_REPRODUCE_RATIO * 100}%.</li>
 *   <li>Yêu cầu có Voi trưởng thành khác cùng đàn trong tầm nhìn.</li>
 * </ul>
 *
 * <h3>Cây kế thừa:</h3>
 * Entity → LivingBeing → Animal → HerbivoreAnimal → Elephant
 *
 * @see HerbivoreAnimal
 * @see Animal
 */
public class Elephant extends HerbivoreAnimal {

    // =========================================================
    // HẰNG SỐ LOÀI
    // =========================================================

    private static final float  SIZE               = 80.0f;   // Lớn nhất hệ sinh thái
    private static final float  BASE_SPEED         = 60.0f;   // Chậm, nhưng không ai dám chặn

    private static final double MAX_HEALTH         = 500.0;   // Bền nhất
    private static final double MAX_HUNGER         = 300.0;   // Ăn nhiều vì cơ thể lớn
    private static final double HUNGER_DECAY_RATE  = 3.0;     // điểm/giây
    private static final double MAX_THIRST         = 250.0;
    private static final double THIRST_DECAY_RATE  = 2.5;     // điểm/giây
    private static final double MAX_AGE            = 3600.0;  // 60 phút mô phỏng — tuổi thọ cao nhất
    private static final double VISION_RANGE       = 350.0;   // Tầm nhìn rộng

    /**
     * Ngưỡng sức khỏe tối thiểu để sinh sản (% maxHealth).
     * Voi cần sức khỏe ≥ 70% mới đủ điều kiện.
     */
    private static final double MIN_HEALTH_TO_REPRODUCE_RATIO = 0.70;

    // =========================================================
    // THUỘC TÍNH RIÊNG
    // =========================================================

    /**
     * ID định danh đàn.
     * Các Voi cùng {@code herdId} di chuyển và sinh sản cùng nhau.
     * {@code herdId = -1} nghĩa là Voi đang đi một mình (chưa có đàn).
     */
    private int herdId;

    private final Random random = new Random();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Khởi tạo một con Voi tại vị trí cho trước, thuộc đàn {@code herdId}.
     *
     * @param position Vị trí sinh ra trên bản đồ
     * @param herdId   ID đàn (dùng -1 nếu chưa có đàn)
     */
    public Elephant(Vector2 position, int herdId) {
        super(position, SIZE, BASE_SPEED);

        // --- Định danh ---
        this.speciesName = "Voi";
        this.herdId      = herdId;

        // --- Chỉ số sinh học ---
        this.maxHealth        = MAX_HEALTH;
        this.health           = MAX_HEALTH;
        this.maxHunger        = MAX_HUNGER;
        this.hunger           = MAX_HUNGER;
        this.hungerDecayRate  = HUNGER_DECAY_RATE;
        this.maxThirst        = MAX_THIRST;
        this.thirst           = MAX_THIRST;
        this.thirstDecayRate  = THIRST_DECAY_RATE;
        this.maxAge           = MAX_AGE;
        this.visionRange      = VISION_RANGE;

        // --- Bộ não mặc định: đi dạo theo đàn ---
        this.setStrategy(new PassiveStrategy());
    }

    /**
     * Constructor không có đàn — Voi đi một mình.
     *
     * @param position Vị trí sinh ra
     */
    public Elephant(Vector2 position) {
        this(position, -1);
    }


    // HÀNH VI ĐẶC TRƯNG
    // =========================================================

    /**
     * Dẫn đầu đàn — tìm tất cả Voi cùng {@code herdId} trong tầm nhìn
     * và đồng bộ hướng di chuyển về phía trọng tâm của đàn.
     *
     * <p>Thuật toán đơn giản (Boids-lite):</p>
     * <ol>
     *   <li>Tính trọng tâm trung bình của các thành viên đàn.</li>
     *   <li>Di chuyển từng bước nhỏ về phía trọng tâm đó.</li>
     * </ol>
     */
    public void leadHerd() {
        if (world == null || world.getSpatialGrid() == null) return;
        if (herdId < 0) return; // Không có đàn → bỏ qua

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        // Thu thập vị trí các thành viên cùng đàn
        float sumX = this.position.x;
        float sumY = this.position.y;
        int   count = 1;

        for (Entity e : neighbors) {
            if (e == this || !e.isAlive()) continue;
            if (e instanceof Elephant) {
                Elephant other = (Elephant) e;
                if (other.herdId == this.herdId) {
                    sumX += other.position.x;
                    sumY += other.position.y;
                    count++;
                }
            }
        }

        if (count <= 1) return; // Chỉ có mình, không cần điều phối

        // Hướng về trọng tâm đàn
        float centroidX = sumX / count;
        float centroidY = sumY / count;

        float dx = centroidX - this.position.x;
        float dy = centroidY - this.position.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Nếu đã ở trung tâm đàn (< 50px), đứng yên
        if (dist < 50.0f) return;

        Vector2 herdDir = new Vector2(dx / dist, dy / dist);
        this.setFacingRight(herdDir.x > 0);
        move(herdDir, 1.0f / 60.0f);
    }

    /**
     * Sinh sản — tạo ra một con Voi con gần vị trí hiện tại.
     *
     * <p>Điều kiện đặc biệt của Voi (khắt khe hơn canReproduce() chung):</p>
     * <ul>
     *   <li>{@link #canReproduce()} == true</li>
     *   <li>Sức khỏe ≥ {@value #MIN_HEALTH_TO_REPRODUCE_RATIO * 100}% maxHealth</li>
     *   <li>Có ít nhất một Voi trưởng thành cùng đàn trong tầm nhìn</li>
     * </ul>
     *
     * @return Con Voi con mới, hoặc {@code null} nếu không đủ điều kiện
     */
    @Override
    public Animal reproduce() {
        if (!canReproduce()) return null;

        // Điều kiện bổ sung: sức khỏe phải đủ cao
        if (health < maxHealth * MIN_HEALTH_TO_REPRODUCE_RATIO) return null;

        // Phải có bạn tình cùng đàn trong tầm nhìn
        Elephant partner = findHerdPartner();
        if (partner == null) return null;

        // Sinh con gần vị trí mẹ (lệch ngẫu nhiên ±60px)
        float offsetX = (random.nextFloat() * 120) - 60;
        float offsetY = (random.nextFloat() * 120) - 60;
        Vector2 birthPos = new Vector2(this.position.x + offsetX, this.position.y + offsetY);

        Elephant offspring = new Elephant(birthPos, this.herdId);

        // Tiêu hao năng lượng đáng kể sau khi sinh (mang thai nặng nề)
        this.health = Math.max(1, this.health - maxHealth * 0.20);
        this.hunger = Math.max(0, this.hunger - maxHunger * 0.35);
        this.thirst = Math.max(0, this.thirst - maxThirst * 0.25);

        System.out.printf("[%s | đàn %d] sinh ra Voi con tại %s%n",
                speciesName, herdId, birthPos);
        return offspring;
    }

    /**
     * Ăn cỏ — override để log đặc trưng cho Voi (logic chính ở HerbivoreAnimal).
     *
     * @param grass Cỏ muốn ăn
     */
    @Override
    public void eatGrass(Grass grass) {
        if (!alive || grass == null || !grass.isAlive()) return;
        super.eatGrass(grass);
    }

    /**
     * Ăn quả rụng — nguồn dinh dưỡng cao hơn cỏ.
     *
     * @param fruit Quả muốn ăn
     */
    @Override
    public void eatFruit(Fruit fruit) {
        if (!alive || fruit == null || !fruit.isAlive()) return;
        super.eatFruit(fruit);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Voi chấp nhận cả {@link Grass} và {@link Fruit}.
     * Dispatcher này kế thừa đầy đủ từ {@link HerbivoreAnimal#eat(Plant)}.</p>
     */
    @Override
    public void eat(Plant food) {
        if (!alive || food == null || !food.isAlive()) return;

        if (food instanceof Grass) {
            eatGrass((Grass) food);
        } else if (food instanceof Fruit) {
            eatFruit((Fruit) food);
        }
    }

    // =========================================================
    // KHÔNG flee() — Voi không bao giờ bỏ chạy
    // =========================================================

    /**
     * Voi không flee — phương thức này bị chặn hoàn toàn.
     * Gọi phương thức này không có tác dụng gì và in cảnh báo.
     *
     * <p>Kẻ thù (Wolf, Tiger) nên kiểm tra kích thước mục tiêu trước khi tấn công.
     * Voi có {@code size = }{@value #SIZE} — lớn nhất hệ sinh thái.</p>
     */
    public final void flee() {
        // Voi không sợ bất kỳ loài nào — không làm gì cả.
        System.out.printf("[%s] không bỏ chạy — Voi không sợ kẻ thù!%n", speciesName);
    }

    // =========================================================
    // KIỂM TRA CÓ THỂ BỊ SĂN KHÔNG
    // =========================================================

    /**
     * Kiểm tra xem một kẻ săn mồi có thể tấn công Voi không.
     * Wolf và Tiger <b>không thể</b> săn Voi.
     * Trả về {@code false} với mọi loài động vật ăn thịt thông thường.
     *
     * @param predator Kẻ tấn công
     * @return {@code false} — Voi không thể bị Wolf/Tiger săn
     */
    public boolean canBeHuntedBy(Animal predator) {
        if (predator == null) return false;
        String cls = predator.getClass().getSimpleName();
        // Wolf và Tiger không thể săn Voi
        if (cls.equals("Wolf") || cls.equals("Tiger")) return false;
        // Hunter (người) có thể — trong thực tế săn voi là có
        return cls.equals("Hunter");
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ NỘI BỘ
    // =========================================================

    /**
     * Tìm một con Voi trưởng thành khác cùng đàn trong tầm nhìn.
     *
     * @return Đối tác Voi cùng đàn, hoặc null nếu không tìm thấy
     */
    private Elephant findHerdPartner() {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        for (Entity e : neighbors) {
            if (e == this || !e.isAlive()) continue;
            if (e instanceof Elephant) {
                Elephant other = (Elephant) e;
                if (other.herdId == this.herdId && other.isAdult()) {
                    return other;
                }
            }
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả thành viên đàn trong tầm nhìn.
     *
     * @return List các Voi cùng herdId (không bao gồm bản thân)
     */
    public List<Elephant> getHerdMembers() {
        List<Elephant> members = new ArrayList<>();
        if (world == null || world.getSpatialGrid() == null) return members;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        for (Entity e : neighbors) {
            if (e == this || !e.isAlive()) continue;
            if (e instanceof Elephant && ((Elephant) e).herdId == this.herdId) {
                members.add((Elephant) e);
            }
        }
        return members;
    }

    // =========================================================
    // RENDER (để trống — RenderSystem đảm nhận)
    // =========================================================

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem vẽ sprite Voi dựa trên facingRight và trạng thái
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public int getHerdId() { return herdId; }

    public void setHerdId(int herdId) { this.herdId = herdId; }

    // =========================================================
    // toString
    // =========================================================

    @Override
    public String toString() {
        return super.toString() + String.format(" | herdId=%d", herdId);
    }
}

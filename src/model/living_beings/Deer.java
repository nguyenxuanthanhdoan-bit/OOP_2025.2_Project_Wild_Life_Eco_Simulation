package model.living_beings;

import core.DisplayMode;
import core.Vector2;
import model.entity.Entity;
import model.plants.Grass;
import model.plants.Plant;
import model.strategies.PassiveStrategy;

import java.util.List;
import java.util.Random;

/**
 * Lớp đại diện cho loài <b>Hươu / Nai</b> trong hệ sinh thái.
 *
 * <h3>Đặc điểm sinh học:</h3>
 * <ul>
 *   <li>Loài ăn cỏ (HERBIVORE) — chỉ ăn {@link Grass}.</li>
 *   <li>Kích thước lớn hơn Rabbit, nhỏ hơn Elephant.</li>
 *   <li>Tuổi thọ trung bình.</li>
 * </ul>
 *
 * <h3>Hành vi xã hội:</h3>
 * <ul>
 *   <li>Di chuyển theo đàn ({@code herdId}) — {@link #followHerd()} bám theo trọng tâm đàn.</li>
 *   <li>Rabbit phải nhường đường cho Deer (Deer lớn hơn, ưu tiên va chạm cao hơn).</li>
 *   <li>Không thể trốn trong Bush (quá to).</li>
 * </ul>
 *
 * <h3>Phản ứng kẻ thù:</h3>
 * <ul>
 *   <li>Phát hiện Wolf, Tiger, Hunter trong tầm nhìn → {@link #scatter()} phá vỡ đội hình,
 *       sau đó {@link #flee(Entity)} chạy theo hướng ngẫu nhiên.</li>
 * </ul>
 *
 * <h3>Sinh sản:</h3>
 * <ul>
 *   <li>Yêu cầu {@code adult == true} và sức khỏe ≥ {@value #MIN_HEALTH_RATIO * 100}%.</li>
 *   <li>Phải có Deer trưởng thành khác cùng đàn trong tầm nhìn.</li>
 * </ul>
 *
 * <h3>Cây kế thừa:</h3>
 * Entity → LivingBeing → Animal → HerbivoreAnimal → Deer
 *
 * @see HerbivoreAnimal
 * @see Animal
 */
public class Deer extends HerbivoreAnimal {

    // =========================================================
    // HẰNG SỐ LOÀI
    // =========================================================

    private static final float  SIZE              = 40.0f;   // Lớn hơn Rabbit (20), nhỏ hơn Elephant (80)
    private static final float  BASE_SPEED        = 130.0f;  // Nhanh — lợi thế thoát thân
    private static final float  FLEE_SPEED_BOOST  = 1.6f;    // Nhân tốc độ khi bỏ chạy

    private static final double MAX_HEALTH        = 150.0;
    private static final double MAX_HUNGER        = 150.0;
    private static final double HUNGER_DECAY_RATE = 2.5;     // điểm/giây
    private static final double MAX_THIRST        = 130.0;
    private static final double THIRST_DECAY_RATE = 2.0;     // điểm/giây
    private static final double MAX_AGE           = 900.0;   // 15 phút mô phỏng — tuổi thọ trung bình
    private static final double VISION_RANGE      = 280.0;   // pixel — tầm nhìn rộng hơn Rabbit

    /** Ngưỡng sức khỏe tối thiểu để sinh sản (%). */
    private static final double MIN_HEALTH_RATIO = 0.60;

    // =========================================================
    // THUỘC TÍNH RIÊNG
    // =========================================================

    /**
     * ID định danh đàn.
     * Các Deer cùng {@code herdId} di chuyển cùng nhau.
     * {@code -1} = đang đi một mình (chưa có đàn).
     */
    private int herdId;

    /**
     * Trạng thái "đang trong đàn".
     * {@code true} khi Deer đang bám theo đàn bình thường.
     * {@code false} khi đã {@link #scatter()} do hoảng loạn.
     */
    private boolean inHerd;

    /** Hướng chạy ngẫu nhiên khi hoảng loạn (được đặt khi scatter()). */
    private Vector2 panicDirection;

    /** Bộ đếm thời gian hoảng loạn (giây) — sau khi hết sẽ quay về đàn. */
    private float panicTimer;

    private static final float PANIC_DURATION = 5.0f; // Chạy loạn 5 giây rồi tìm đàn lại

    private final Random random = new Random();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Khởi tạo một con Deer tại vị trí cho trước, thuộc đàn {@code herdId}.
     *
     * @param position Vị trí sinh ra trên bản đồ
     * @param herdId   ID đàn ({@code -1} nếu chưa có đàn)
     */
    public Deer(Vector2 position, int herdId) {
        super(position, SIZE, BASE_SPEED);

        // --- Định danh ---
        this.speciesName = "Hươu";
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

        // --- Trạng thái đàn ---
        this.inHerd       = (herdId >= 0);
        this.panicDirection = new Vector2();
        this.panicTimer    = 0f;

        // --- Bộ não mặc định ---
        this.setStrategy(new PassiveStrategy());
    }

    /** Constructor không có đàn — Deer đi một mình. */
    public Deer(Vector2 position) {
        this(position, -1);
    }



    // =========================================================
    // HÀNH VI ĐẶC TRƯNG
    // =========================================================

    /**
     * Bám theo đàn — di chuyển về phía trọng tâm trung bình của các
     * Deer cùng {@code herdId} trong tầm nhìn.
     *
     * <p>Nếu tất cả thành viên đã tụ tại trọng tâm (khoảng cách < 60px),
     * Deer đứng yên (không tạo chuyển động vô nghĩa).</p>
     */
    public void followHerd() {
        if (world == null || world.getSpatialGrid() == null || herdId < 0) return;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        float sumX = this.position.x;
        float sumY = this.position.y;
        int   count = 1;

        for (Entity e : neighbors) {
            if (e == this || !e.isAlive()) continue;
            if (e instanceof Deer) {
                Deer other = (Deer) e;
                if (other.herdId == this.herdId) {
                    sumX += other.position.x;
                    sumY += other.position.y;
                    count++;
                }
            }
        }

        if (count <= 1) return; // Chỉ có mình

        float centroidX = sumX / count;
        float centroidY = sumY / count;
        float dx = centroidX - this.position.x;
        float dy = centroidY - this.position.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 60.0f) return; // Đã ở gần trọng tâm đủ

        Vector2 dir = new Vector2(dx / dist, dy / dist);
        this.setFacingRight(dir.x > 0);
        move(dir, 1.0f / 60.0f);
    }

    /**
     * Phá vỡ đội hình đàn khi phát hiện kẻ thù.
     * Đặt {@code inHerd = false}, tạo hướng chạy ngẫu nhiên và bật {@code panicTimer}.
     */
    public void scatter() {
        this.inHerd = false;
        this.panicTimer = PANIC_DURATION;

        // Hướng chạy ngẫu nhiên
        float angle = random.nextFloat() * 2 * (float) Math.PI;
        this.panicDirection = new Vector2(
                (float) Math.cos(angle),
                (float) Math.sin(angle)
        );

        System.out.printf("[%s | đàn %d] đội hình bị phá vỡ! Chạy loạn!%n", speciesName, herdId);
    }

    /**
     * Bỏ chạy ngược hướng kẻ thù.
     * Tăng tốc độ {@value #FLEE_SPEED_BOOST}x trong 1 bước.
     *
     * @param threat Kẻ thù đe dọa (Wolf / Tiger / Hunter)
     */
    public void flee(Entity threat) {
        if (!alive || threat == null) return;

        Vector2 fleeDir = this.position.copy()
                .subtract(threat.getPosition())
                .normalize();

        float savedSpeed = this.speed;
        this.speed = BASE_SPEED * FLEE_SPEED_BOOST;
        this.setFacingRight(fleeDir.x > 0);
        move(fleeDir, 1.0f / 60.0f);
        this.speed = savedSpeed;

        System.out.printf("[%s] bỏ chạy khỏi %s%n",
                speciesName, threat.getClass().getSimpleName());
    }

    /**
     * Sinh sản — tạo ra một con Deer con gần vị trí Deer mẹ.
     *
     * <p>Điều kiện:</p>
     * <ul>
     *   <li>{@link #canReproduce()} == true</li>
     *   <li>Sức khỏe ≥ {@value #MIN_HEALTH_RATIO * 100}% maxHealth</li>
     *   <li>Có Deer trưởng thành cùng đàn trong tầm nhìn</li>
     * </ul>
     *
     * @return Con Deer con mới, hoặc {@code null} nếu không đủ điều kiện
     */
    @Override
    public Animal reproduce() {
        if (!canReproduce()) return null;
        if (health < maxHealth * MIN_HEALTH_RATIO) return null;
        if (world == null) return null;

        Deer partner = findHerdPartner();
        if (partner == null) return null;

        float offsetX = (random.nextFloat() * 80) - 40;
        float offsetY = (random.nextFloat() * 80) - 40;
        Vector2 birthPos = new Vector2(this.position.x + offsetX, this.position.y + offsetY);

        Deer offspring = new Deer(birthPos, this.herdId);

        this.hunger = Math.max(0, this.hunger - maxHunger * 0.25);
        this.thirst = Math.max(0, this.thirst - maxThirst * 0.20);
        this.health = Math.max(1, this.health - maxHealth * 0.15);

        System.out.printf("[%s | đàn %d] sinh ra Hươu con tại %s%n",
                speciesName, herdId, birthPos);
        return offspring;
    }

    // =========================================================
    // OVERRIDE — Chỉ ăn Grass
    // =========================================================

    /**
     * Deer chỉ ăn {@link Grass} — bỏ qua mọi loại Plant khác.
     *
     * @param food Thức ăn
     */
    @Override
    public void eat(Plant food) {
        if (!alive) return;
        if (food instanceof Grass) {
            eatGrass((Grass) food);
        }
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ NỘI BỘ
    // =========================================================

    /**
     * Quét SpatialGrid tìm kẻ thù (Wolf / Tiger / Hunter) trong tầm nhìn.
     *
     * @return Entity kẻ thù gần nhất, hoặc null nếu an toàn
     */
    private Entity detectThreat() {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        Entity closestThreat = null;
        float minDistSq = Float.MAX_VALUE;

        for (Entity e : neighbors) {
            if (!e.isAlive() || e == this) continue;
            String cls = e.getClass().getSimpleName();
            if (cls.equals("Wolf") || cls.equals("Tiger") || cls.equals("Hunter")) {
                float dSq = this.position.distanceSquared(e.getPosition());
                if (dSq < minDistSq) {
                    minDistSq = dSq;
                    closestThreat = e;
                }
            }
        }
        return closestThreat;
    }

    /**
     * Tìm Deer trưởng thành cùng đàn trong tầm nhìn để sinh sản.
     */
    private Deer findHerdPartner() {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        for (Entity e : neighbors) {
            if (e == this || !e.isAlive()) continue;
            if (e instanceof Deer) {
                Deer other = (Deer) e;
                if (other.herdId == this.herdId && other.isAdult()) {
                    return other;
                }
            }
        }
        return null;
    }

    // =========================================================
    // RENDER
    // =========================================================

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem đảm nhận vẽ sprite Deer
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public int getHerdId()           { return herdId; }
    public void setHerdId(int id)    { this.herdId = id; this.inHerd = (id >= 0); }

    public boolean isInHerd()        { return inHerd; }
    public void setInHerd(boolean v) { this.inHerd = v; }

    // =========================================================
    // toString
    // =========================================================

    @Override
    public String toString() {
        return super.toString() + String.format(" | herdId=%d | inHerd=%b | panic=%.1fs",
                herdId, inHerd, panicTimer);
    }
}

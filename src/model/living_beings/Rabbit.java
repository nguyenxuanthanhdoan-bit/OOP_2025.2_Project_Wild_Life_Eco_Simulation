package model.living_beings;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.plants.Grass;
import model.plants.Plant;
import model.strategies.PassiveStrategy;
import model.structures.Bush;
import model.world.World;

import java.util.List;
import java.util.Random;

/**
 * Lớp đại diện cho loài <b>Thỏ</b> trong hệ sinh thái.
 *
 * <h3>Đặc điểm sinh học:</h3>
 * <ul>
 *   <li>Loài ăn cỏ (HERBIVORE) — chỉ ăn {@link Grass}.</li>
 *   <li>Máu thấp nhất trong các loài động vật.</li>
 *   <li>Tốc độ cao — lợi thế duy nhất khi bị truy đuổi.</li>
 *   <li>Tuổi thọ ngắn.</li>
 * </ul>
 *
 * <h3>Hành vi:</h3>
 * <ul>
 *   <li>Phát hiện kẻ thù (Wolf, Tiger, Hunter) trong {@code visionRange} → {@link #flee(Entity)}.</li>
 *   <li>Nếu có Bush gần → {@link #hideInBush(Bush)} để ẩn nấp.</li>
 *   <li>Khi đói/khát nghiêm trọng (< 30%) → {@link #leaveBush()} dù còn nguy hiểm.</li>
 *   <li>Sinh sản khi đủ điều kiện và có bạn tình cùng loài trong vùng nhìn.</li>
 * </ul>
 *
 * <h3>Cây kế thừa:</h3>
 * Entity → LivingBeing → Animal → HerbivoreAnimal → Rabbit
 *
 * @see HerbivoreAnimal
 * @see Animal
 */
public class Rabbit extends HerbivoreAnimal {

    // =========================================================
    // HẰNG SỐ LOÀI
    // =========================================================

    private static final float  SIZE              = 20.0f;
    private static final float  BASE_SPEED        = GameConfig.getInstance().RABBIT_BASE_SPEED;
    private static final float  FLEE_SPEED_BOOST  = 1.8f;   // Nhân tốc độ khi bỏ chạy

    private static final double MAX_HEALTH        = 50.0;   // Máu thấp nhất
    private static final double MAX_HUNGER        = 100.0;
    private static final double HUNGER_DECAY_RATE = 2.0;    // điểm/giây khi đứng yên
    private static final double MAX_THIRST        = 100.0;
    private static final double THIRST_DECAY_RATE = 3.0;    // điểm/giây (khát nhanh hơn đói)
    private static final double MAX_AGE           = 300.0;  // 5 phút mô phỏng — tuổi thọ ngắn
    private static final double VISION_RANGE      = 200.0;  // pixel

    /** Ngưỡng đói/khát "nghiêm trọng" (%) — dưới mức này sẽ rời Bush dù nguy hiểm. */
    private static final double CRITICAL_NEED_THRESHOLD = 0.30;

    // =========================================================
    // THUỘC TÍNH RIÊNG
    // =========================================================

    /**
     * Trạng thái ẩn nấp trong Bush.
     * Khi {@code hidden == true}: thỏ không di chuyển và kẻ thù không
     * thể "nhìn thấy" thỏ (logic phát hiện bên ngoài kiểm tra trường này).
     */
    private boolean hidden;

    /** Bush đang ẩn náu (null nếu không đang trốn). */
    private Bush currentBush;

    private final Random random = new Random();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Khởi tạo một con Thỏ tại vị trí cho trước.
     *
     * @param position Vị trí sinh ra trên bản đồ
     */
    public Rabbit(Vector2 position) {
        super(position, SIZE, BASE_SPEED);

        // --- Định danh ---
        this.speciesName = "Thỏ";

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

        // --- Trạng thái ---
        this.hidden     = false;
        this.currentBush = null;

        // --- Bộ não mặc định: đi dạo ngẫu nhiên ---
        this.setStrategy(new PassiveStrategy());
    }

    // =========================================================
    // VÒNG ĐỜI — update()
    // =========================================================



    // =========================================================
    // HÀNH VI ĐẶC TRƯNG
    // =========================================================

    /**
     * Chui vào Bush để ẩn nấp.
     * <ul>
     *   <li>Đặt {@code hidden = true}.</li>
     *   <li>Đánh dấu Bush là {@code occupied}.</li>
     *   <li>Dừng di chuyển (tốc độ về 0).</li>
     * </ul>
     *
     * @param bush Bush mục tiêu (không null, phải chưa bị chiếm)
     */
    public void hideInBush(Bush bush) {
        if (bush == null || bush.isOccupied()) return;

        this.currentBush = bush;
        bush.setOccupied(true);
        this.hidden = true;
        this.speed = 0;   // Đứng im hoàn toàn khi ẩn

        System.out.printf("[%s] đã chui vào %s để ẩn nấp!%n", speciesName, bush);
    }

    /**
     * Rời khỏi Bush đang ẩn náu.
     * <ul>
     *   <li>Đặt {@code hidden = false}.</li>
     *   <li>Giải phóng Bush ({@code occupied = false}).</li>
     *   <li>Khôi phục tốc độ về mức cơ bản.</li>
     * </ul>
     */
    public void leaveBush() {
        if (!hidden || currentBush == null) return;

        currentBush.setOccupied(false);
        this.currentBush = null;
        this.hidden = false;
        this.speed = this.baseSpeed;   // Khôi phục tốc độ

        System.out.printf("[%s] đã rời khỏi Bush.%n", speciesName);
    }

    /**
     * Bỏ chạy khỏi kẻ thù.
     * Hướng chạy = ngược hướng từ kẻ thù đến thỏ, chuẩn hóa.
     * Tốc độ tạm thời tăng {@value #FLEE_SPEED_BOOST}x.
     *
     * @param threat Thực thể đang đe dọa (Wolf / Tiger / Hunter)
     */
    public void flee(Entity threat) {
        if (!alive || threat == null) return;

        // Hướng thoát = vị trí thỏ - vị trí kẻ thù (ngược chiều)
        Vector2 fleeDir = this.position.copy()
                .subtract(threat.getPosition())
                .normalize();

        // Tăng tốc độ tạm thời khi bỏ chạy
        float savedSpeed = this.speed;
        this.speed = this.baseSpeed * FLEE_SPEED_BOOST;

        // Cập nhật hướng mặt
        this.setFacingRight(fleeDir.x > 0);

        move(fleeDir, 1.0f / 60.0f);   // Tính cho 1 frame (deltaTime quản lý bên ngoài)

        this.speed = savedSpeed;

        System.out.printf("[%s] bỏ chạy khỏi %s theo hướng %s%n",
                speciesName, threat.getClass().getSimpleName(), fleeDir);
    }

    /**
     * Sinh sản — tạo ra một con Thỏ con tại vị trí gần thỏ mẹ.
     *
     * <p>Điều kiện:</p>
     * <ul>
     *   <li>{@link #canReproduce()} == true</li>
     *   <li>Có ít nhất một Thỏ trưởng thành khác trong {@code visionRange}</li>
     * </ul>
     *
     * @return Con Thỏ con mới, hoặc {@code null} nếu không đủ điều kiện
     */
    @Override
    public Animal reproduce() {
        if (!canReproduce()) return null;
        if (world == null) return null;

        // Tìm bạn tình trong tầm nhìn
        Rabbit partner = findMatingPartner();
        if (partner == null) return null;

        // Sinh con gần thỏ mẹ (lệch ngẫu nhiên ±30px)
        float offsetX = (random.nextFloat() * 60) - 30;
        float offsetY = (random.nextFloat() * 60) - 30;
        Vector2 birthPos = new Vector2(this.position.x + offsetX, this.position.y + offsetY);

        Rabbit offspring = new Rabbit(birthPos);

        // Tiêu hao năng lượng sau khi sinh
        this.hunger  = Math.max(0, this.hunger  - maxHunger  * 0.3);
        this.thirst  = Math.max(0, this.thirst  - maxThirst  * 0.2);
        this.health  = Math.max(1, this.health  - maxHealth  * 0.1);

        System.out.printf("[%s] sinh ra một con Thỏ con tại %s%n", speciesName, birthPos);
        return offspring;
    }

    // =========================================================
    // OVERRIDE — Chỉ ăn Grass
    // =========================================================

    /**
     * Thỏ chỉ ăn được {@link Grass}.
     * Nếu truyền vào loại Plant khác, bỏ qua.
     *
     * @param food Thức ăn (chỉ chấp nhận Grass)
     */
    @Override
    public void eat(Plant food) {
        if (!alive) return;
        if (food instanceof Grass) {
            super.eat(food);
        }
        // Bỏ qua các loại thực vật khác (FruitTree, v.v.)
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ NỘI BỘ
    // =========================================================

    /**
     * Quét tất cả Entity trong {@code visionRange} để tìm kẻ thù nguy hiểm nhất.
     * Kẻ thù được nhận diện theo tên lớp: Wolf, Tiger, Hunter.
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
            if (isThreat(e)) {
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
     * Nhận diện kẻ thù theo tên lớp đơn giản.
     * Mở rộng sau này bằng cách dùng {@code instanceof} khi có class Wolf/Tiger/Hunter.
     */
    private boolean isThreat(Entity e) {
        String cls = e.getClass().getSimpleName();
        return cls.equals("Wolf") || cls.equals("Tiger") || cls.equals("Hunter");
    }

    /**
     * Tìm Bush chưa bị chiếm gần nhất trong tầm nhìn.
     *
     * @return Bush phù hợp, hoặc null nếu không có
     */
    private Bush findNearbyBush() {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        for (Entity e : neighbors) {
            // Bush chưa phải Entity, dùng entities list trực tiếp từ World
            // Khi Bush được tích hợp vào Entity trong tương lai sẽ kiểm tra ở đây
        }

        // Fallback: duyệt entity list của world tìm Bush (tương thích hiện tại)
        for (Entity e : world.getEntities()) {
            if (e instanceof Bush) {
                Bush b = (Bush) e;
                if (!b.isOccupied() && b.contains(this.position)) {
                    return b;
                }
            }
        }
        return null;
    }

    /**
     * Tìm con Thỏ trưởng thành khác trong tầm nhìn để sinh sản cùng.
     *
     * @return Rabbit đối tác, hoặc null nếu không tìm thấy
     */
    private Rabbit findMatingPartner() {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid()
                .getNeighbors(this.position, (float) visionRange);

        for (Entity e : neighbors) {
            if (e == this) continue;
            if (e instanceof Rabbit) {
                Rabbit other = (Rabbit) e;
                if (other.isAlive() && other.isAdult() && !other.isHidden()) {
                    return other;
                }
            }
        }
        return null;
    }

    /**
     * Kiểm tra xem tình trạng đói/khát có đến mức phải rời Bush không.
     *
     * @return true nếu đói hoặc khát dưới ngưỡng {@value #CRITICAL_NEED_THRESHOLD}
     */
    private boolean shouldLeaveBushDueToNeed() {
        boolean criticallyHungry = hunger  < maxHunger  * CRITICAL_NEED_THRESHOLD;
        boolean criticallyThirsty = thirst < maxThirst  * CRITICAL_NEED_THRESHOLD;
        return criticallyHungry || criticallyThirsty;
    }

    // =========================================================
    // RENDER (để trống — RenderSystem đảm nhận)
    // =========================================================

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ vẽ sprite dựa trên trạng thái (hidden, facingRight, v.v.)
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    /**
     * @return true nếu Thỏ đang ẩn trong Bush
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Đặt trạng thái ẩn nấp thủ công (dùng cho serialization / testing).
     * Trong gameplay, ưu tiên dùng {@link #hideInBush(Bush)} / {@link #leaveBush()}.
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Bush getCurrentBush() {
        return currentBush;
    }

    // =========================================================
    // toString
    // =========================================================

    @Override
    public String toString() {
        return super.toString() + String.format(" | hidden=%b", hidden);
    }
}
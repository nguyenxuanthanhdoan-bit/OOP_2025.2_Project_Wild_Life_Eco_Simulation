package model.structures;

import core.DisplayMode;
import core.Vector2;
import model.entity.Entity;
import java.util.Random;

/**
 * Bụi cây — cấu trúc môi trường cho phép động vật nhỏ ẩn nấp.
 *
 * <p>Kế thừa {@link Entity} để có thể tham gia vào {@code World.entities},
 * được quản lý bởi {@code SpatialGrid} và hiển thị qua {@code RenderSystem}.</p>
 *
 * <p>Rabbit có thể chui vào Bush để tránh kẻ thù.
 * Khi {@code occupied == true}, Bush đang có một sinh vật bên trong.</p>
 */
public class Bush extends Entity {

    /** Bán kính vùng bụi (pixel). Thú vào trong bán kính này thì coi như "trong bụi". */
    private float radius;

    /** Có sinh vật đang ẩn nấp bên trong không. */
    private boolean occupied;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * @param position Vị trí trung tâm của Bush
     * @param radius   Bán kính phủ sóng (pixel)
     */
    public Bush(Vector2 position, float radius) {
        super(position, radius);   // size = radius để SpatialGrid / render dùng đúng
        this.radius   = radius;
        this.occupied = false;
        this.isSolid  = false; // Bụi rậm là thực thể mềm, thú có thể đi xuyên qua
        this.collider = new model.collision.Collider(this, this.radius * 0.5f, model.collision.CollisionLayer.OBSTACLE);

        // Random hình ảnh từ Bush_1 đến Bush_2
        Random random = new Random();
        int variant = random.nextInt(2) + 1;
        this.imageVariant = "Bush_" + variant;
    }

    /** Constructor với bán kính mặc định 25px. */
    public Bush(Vector2 position) {
        this(position, 25.0f);
    }

    // =========================================================
    // ENTITY OVERRIDES
    // =========================================================

    /** Bush không có logic cập nhật — tĩnh trên bản đồ. */
    @Override
    public void update(float deltaTime) {
        // Bush là cấu trúc tĩnh, không cần cập nhật
    }

    /** Render để trống — RenderSystem đảm nhận vẽ sprite Bush. */
    @Override
    public void render(DisplayMode mode) {
        // RenderSystem vẽ sprite Bush dựa trên vị trí và imageVariant
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // =========================================================

    /**
     * Kiểm tra một tọa độ có nằm trong vùng phủ sóng của Bush không.
     *
     * @param pos Vị trí cần kiểm tra
     * @return true nếu {@code pos} nằm trong bán kính của Bush
     */
    public boolean contains(Vector2 pos) {
        return this.position.distanceTo(pos) <= this.radius;
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    @Override
    public String toString() {
        return String.format("Bush[pos=%s | r=%.1f | occupied=%b]", position, radius, occupied);
    }
}

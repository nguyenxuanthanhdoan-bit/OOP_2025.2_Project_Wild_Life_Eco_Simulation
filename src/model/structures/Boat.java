package model.structures;

import core.Vector2;
import model.entity.Structure;

/**
 * Thuyền đánh cá — nằm cố định trên mặt nước.
 *
 * Thuyền là vật trang trí (không rắn, không cản đường)
 * vì Human không bước lên thuyền được trong nước.
 * Dùng làm điểm thu hút (Point of Interest) cho AI ban ngày.
 *
 * SpriteKey: "boat" → render từ resources/assets/images/village/boat.png
 */
public class Boat extends Structure {

    /** Kích thước hiển thị của thuyền (pixels). */
    public static final float BOAT_SIZE = 56.0f;

    /** Bán kính "khu vực bến thuyền" — Human sẽ đến gần đây, không vào trong nước. */
    public static final float DOCK_RADIUS = 80.0f;

    public Boat(Vector2 position) {
        super(position, BOAT_SIZE, "BOAT", "boat", false); // solid=false: không cản đường
    }

    /**
     * Trả về vị trí bờ gần nhất để Human đứng ngắm thuyền.
     * Mặc định trả về vị trí thuyền — AI sẽ đi đến gần bờ (bị nước chặn trước).
     */
    public Vector2 getPosition() {
        return position.copy();
    }
}

package model.structures;

import core.Vector2;
import model.entity.Structure;

/**
 * Nhà chài — công trình ven biển gần mép nước.
 *
 * Nhà chài là vật cản cứng (solid=true) như các nhà bình thường.
 * Là điểm thu hút mạnh (Point of Interest) cho Human ban ngày —
 * Human có xu hướng đến đây để "đánh cá" hoặc nghỉ ngơi.
 *
 * SpriteKey: "fishing_hut" → render từ resources/assets/images/village/fishing_hut.png
 */
public class FishingHut extends Structure {

    /** Kích thước hiển thị của nhà chài (pixels). */
    public static final float FISHING_HUT_SIZE = 60.0f;

    /** Bán kính vùng hoạt động quanh nhà chài (Human lang thang quanh đây). */
    public static final float ACTIVITY_RADIUS = 120.0f;

    public FishingHut(Vector2 position) {
        super(position, FISHING_HUT_SIZE, "FISHING_HUT", "fishing_hut", true);
        // Tăng bán kính va chạm để ngăn động vật đi xuyên qua góc nhà
        this.setCollider(new model.collision.Collider(this, FISHING_HUT_SIZE * 0.6f, model.collision.CollisionLayer.OBSTACLE));
    }
}

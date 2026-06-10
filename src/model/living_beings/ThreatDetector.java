package model.living_beings;

import core.GameConfig;
import model.entity.Entity;
import model.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * ThreatDetector — Hệ thống phát hiện mối đe dọa cho Human.
 *
 * Thiết kế:
 *   - Không hard-code tên loài ("Sói", "Hổ", ...).
 *   - Dựa vào {@link model.entity.Entity#getEntityLevel()} để phân loại:
 *     bất kỳ con vật nào có entityLevel cao hơn Human đều là mối đe dọa.
 *   - Sử dụng SpatialGrid để tìm kiếm hiệu quả trong bán kính THREAT_RADIUS.
 *
 * API:
 *   {@code ThreatDetector.detectThreats(human, world)} — danh sách động vật đe dọa
 *   {@code ThreatDetector.hasThreat(human, world)}     — kiểm tra nhanh có mối đe dọa không
 */
public final class ThreatDetector {

    private ThreatDetector() {}

    /**
     * Phát hiện tất cả động vật nguy hiểm trong bán kính THREAT_RADIUS quanh human.
     *
     * Điều kiện là mối đe dọa:
     *   1. Là {@link Animal} và còn sống, không bị ẩn.
     *   2. entityLevel cao hơn human (tức là kẻ săn mồi cấp cao hơn).
     *   3. Không phải chính human đó.
     *
     * @param human Human cần kiểm tra
     * @param world World hiện tại (để truy cập SpatialGrid)
     * @return Danh sách động vật đe dọa (có thể rỗng, không bao giờ null)
     */
    public static List<Animal> detectThreats(Human human, World world) {
        List<Animal> threats = new ArrayList<>();
        if (human == null || world == null || world.getSpatialGrid() == null) return threats;

        float radius = GameConfig.getInstance().THREAT_RADIUS;
        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(human.getPosition(), radius);
        int humanLevel = human.getEntityLevel();

        for (Entity entity : neighbors) {
            if (entity == human) continue;
            if (!(entity instanceof Animal)) continue;
            Animal animal = (Animal) entity;
            if (!animal.isAliveState() || animal.isHidden()) continue;
            if (animal.getEntityLevel() > humanLevel) {
                threats.add(animal);
            }
        }

        return threats;
    }

    /**
     * Kiểm tra nhanh xem có mối đe dọa nào trong tầm không.
     * Hiệu quả hơn {@link #detectThreats} vì dừng sớm khi tìm thấy đầu tiên.
     *
     * @param human Human cần kiểm tra
     * @param world World hiện tại
     * @return {@code true} nếu có ít nhất 1 mối đe dọa
     */
    public static boolean hasThreat(Human human, World world) {
        if (human == null || world == null || world.getSpatialGrid() == null) return false;

        float radius = GameConfig.getInstance().THREAT_RADIUS;
        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(human.getPosition(), radius);
        int humanLevel = human.getEntityLevel();

        for (Entity entity : neighbors) {
            if (entity == human) continue;
            if (!(entity instanceof Animal)) continue;
            Animal animal = (Animal) entity;
            if (!animal.isAliveState() || animal.isHidden()) continue;
            if (animal.getEntityLevel() > humanLevel) return true;
        }

        return false;
    }
}

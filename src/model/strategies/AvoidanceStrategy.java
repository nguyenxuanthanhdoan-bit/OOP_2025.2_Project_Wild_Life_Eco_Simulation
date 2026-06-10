package model.strategies;

import core.Vector2;
import model.living_beings.Animal;
import model.world.World;
import model.entity.Entity;
import java.util.List;

/**
 * Module Steering Behaviors dùng chung.
 *
 * Cung cấp 2 lực:
 *  1. getSolidObstacleForce() — né vật cản tĩnh (Đá, Cây có isSolid=true)
 *     Bắt đầu tác động khi vật cản cách ~(bán kính + 50px).
 *     Dùng lực bẻ lái (steering) kết hợp đẩy thẳng + vuông góc,
 *     giúp con vật uốn lượn mềm mại quanh chướng ngại vật.
 *
 *  2. getLargeAnimalAvoidanceForce() — né các loài thú khổng lồ hơn đáng kể
 *     (giữ nguyên hành vi cũ, đổi tên cho rõ ràng).
 *
 * Cả hai đều là static methods — gọi trực tiếp từ mọi strategy.
 */
public class AvoidanceStrategy {

    /** Ngưỡng kích thước: chỉ né thú lớn hơn N lần */
    private static final float SIZE_THRESHOLD = 1.5f;

    // =========================================================
    // 1. NÉ VẬT CẢN TĨNH (Đá, Cây)
    // =========================================================

    /**
     * Tính lực lái tránh các vật cản tĩnh (isSolid = true).
     *
     * @param owner     Con vật cần né
     * @param world     World hiện tại
     * @param moveDir   Hướng di chuyển hiện tại (đã normalize) — dùng để chọn hướng vòng qua (trái/phải)
     * @return          Lực steering tránh né (chưa normalize, hãy cộng vào desiredDir rồi normalize)
     */
    public static Vector2 getSolidObstacleForce(Animal owner, World world, Vector2 moveDir) {
        Vector2 force = new Vector2();
        if (world == null || world.getSpatialGrid() == null) return force;

        // Quét trong phạm vi: bán kính bản thân + 25px buffer
        float scanRadius = owner.getSize() / 2 + 25f;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(owner.getPosition(), scanRadius);

        for (Entity e : nearby) {
            if (e == owner || !e.isSolid() || !e.isAlive()) continue;

            Vector2 toObs = e.getPosition().copy().subtract(owner.getPosition());
            float dist = toObs.length();
            if (dist < 0.01f) continue;

            float minSep   = owner.getSize() / 2 + e.getSize() / 2; // Khi 2 biên chạm nhau
            float safeZone = minSep + 25f;                           // Bắt đầu né từ đây

            if (dist < safeZone) {
                // Tỉ lệ nguy hiểm: 0 (ở ranh giới an toàn) → 1 (chạm mặt)
                float t = 1f - (dist - minSep) / 25f;
                t = Math.max(0f, Math.min(1f, t));
                float strength = t * t * 3.0f; // Quadratic: tăng nhanh khi tiến gần

                // Hướng đẩy thẳng (ra xa vật cản)
                Vector2 pushBack = toObs.copy().normalize().scale(-strength);

                // Hướng vuông góc (trượt ngang): chọn bên nào "thuận" hơn với hướng đi
                Vector2 perp1 = new Vector2(-toObs.y / dist, toObs.x / dist);
                Vector2 perp2 = new Vector2(toObs.y / dist, -toObs.x / dist);

                // Chọn perpendicular nào cùng chiều với hướng di chuyển hơn
                Vector2 perp;
                if (moveDir != null && moveDir.lengthSquared() > 0.001f) {
                    float dot1 = perp1.x * moveDir.x + perp1.y * moveDir.y;
                    float dot2 = perp2.x * moveDir.x + perp2.y * moveDir.y;
                    perp = (dot1 >= dot2) ? perp1 : perp2;
                } else {
                    perp = perp1; // mặc định
                }

                // Lực = 70% đẩy ra + 30% trượt ngang (uốn cong quỹ đạo)
                force.add(pushBack.scale(0.7f));
                force.add(perp.scale(strength * 0.3f));
            }
        }

        return force;
    }

    /**
     * Phiên bản rút gọn không cần moveDir (dùng cho các trường hợp đơn giản).
     */
    public static Vector2 getSolidObstacleForce(Animal owner, World world) {
        return getSolidObstacleForce(owner, world, null);
    }

    // =========================================================
    // 2. NÉ LOÀI THÚ KHỔNG LỒ (giữ hành vi cũ, đổi tên + giữ alias)
    // =========================================================

    /**
     * Tính lực né tránh các thú lớn hơn đáng kể (Voi, Hổ vs Thỏ...).
     */
    public static Vector2 getLargeAnimalAvoidanceForce(Animal owner, World world) {
        Vector2 avoidanceForce = new Vector2();
        if (world == null || world.getSpatialGrid() == null) return avoidanceForce;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(
            owner.getPosition(), (float) owner.getVisionRange());

        for (Entity e : neighbors) {
            if (!(e instanceof Animal) || e == owner || !e.isAlive()) continue;
            Animal other = (Animal) e;

            if (other.getSize() > owner.getSize() * SIZE_THRESHOLD) {
                Vector2 diff = owner.getPosition().copy().subtract(other.getPosition());
                float dist = diff.length();
                float safeDist = owner.getSize() + other.getSize() + 50.0f;

                if (dist > 0 && dist < safeDist) {
                    Vector2 perpendicular = new Vector2(-diff.y, diff.x).normalize();
                    float strength = (safeDist - dist) / safeDist * 2.0f;
                    avoidanceForce.add(perpendicular.scale(strength));
                    diff.normalize();
                    avoidanceForce.add(diff.scale(strength * 0.5f));
                }
            }
        }

        return avoidanceForce;
    }

    /**
     * Alias giữ backward-compatibility với code cũ đang gọi getAvoidanceForce().
     * Gọi cả ba lực: né thú lớn + né vật cản tĩnh + né biên bản đồ.
     */
    public static Vector2 getAvoidanceForce(Animal owner, World world) {
        return getAvoidanceForce(owner, world, null);
    }

    public static Vector2 getAvoidanceForce(Animal owner, World world, Vector2 moveDir) {
        Vector2 total = getNonWaterAvoidanceForce(owner, world, moveDir);
        total.add(getWaterAvoidanceForce(owner, world, moveDir));
        return total;
    }

    public static Vector2 getNonWaterAvoidanceForce(Animal owner, World world, Vector2 moveDir) {
        Vector2 total = new Vector2();
        total.add(getLargeAnimalAvoidanceForce(owner, world));
        total.add(getSolidObstacleForce(owner, world, moveDir));
        total.add(getBoundaryAvoidanceForce(owner, world));
        return total;
    }

    public static Vector2 getWaterAvoidanceForce(Animal owner, World world, Vector2 moveDir) {
        Vector2 force = new Vector2();
        if (owner == null || world == null || moveDir == null || moveDir.lengthSquared() < 0.001f) return force;

        Vector2 dir = moveDir.copy().normalize();
        float[] distances = {32.0f, 64.0f, 96.0f};
        float[] angles = {0.0f, -35.0f, 35.0f};

        for (float distance : distances) {
            for (float angle : angles) {
                Vector2 probeDir = rotate(dir, angle);
                Vector2 probe = owner.getPosition().copy().add(probeDir.scale(distance));
                boolean isWater = world.isPositionInWater(probe.x, probe.y);
                boolean isFish = owner instanceof model.living_beings.Fish;
                boolean shouldAvoid = isFish ? !isWater : isWater;

                if (shouldAvoid || !world.isValidPositionFor(owner, probe)) {
                    Vector2 away = owner.getPosition().copy().subtract(probe);
                    if (away.lengthSquared() > 0) {
                        away.normalize();
                        float strength = (1.0f - Math.min(distance / 128.0f, 1.0f)) * 2.5f;
                        force.add(away.scale(strength));
                    }
                }
            }
        }

        return force;
    }

    private static Vector2 rotate(Vector2 v, float degrees) {
        double rad = Math.toRadians(degrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new Vector2(v.x * cos - v.y * sin, v.x * sin + v.y * cos);
    }

    // =========================================================
    // 3. NÉ BIÊN BẢN ĐỒ (Bờ biển, tường vô hình)
    // =========================================================

    /**
     * Tính lực đẩy khi con vật tiến gần biên bản đồ (trái/phải/trên/dưới).
     * Hoạt động giống hệt né đá — bắt đầu đẩy từ 25px trước khi chạm biên.
     * Tự động áp dụng cho mọi strategy thông qua getAvoidanceForce().
     *
     * @param owner  Con vật cần né
     * @param world  World hiện tại (lấy width/height)
     * @return       Lực đẩy vào giữa bản đồ
     */
    public static Vector2 getBoundaryAvoidanceForce(Animal owner, World world) {
        Vector2 force = new Vector2();
        if (world == null) return force;

        float x = owner.getPosition().x;
        float y = owner.getPosition().y;
        float radius = owner.getSize() / 2;
        float safeZone = 25f; // Bắt đầu đẩy khi cách biên 25px

        float worldW = world.getWidth();
        float worldH = world.getHeight();

        // Biên trái
        float distLeft = x - radius;
        if (distLeft < safeZone) {
            float t = 1f - distLeft / safeZone;
            t = Math.max(0f, Math.min(1f, t));
            force.x += t * t * 3.0f; // Đẩy sang phải
        }

        // Biên phải
        float distRight = worldW - (x + radius);
        if (distRight < safeZone) {
            float t = 1f - distRight / safeZone;
            t = Math.max(0f, Math.min(1f, t));
            force.x -= t * t * 3.0f; // Đẩy sang trái
        }

        // Biên trên
        float distTop = y - radius;
        if (distTop < safeZone) {
            float t = 1f - distTop / safeZone;
            t = Math.max(0f, Math.min(1f, t));
            force.y += t * t * 3.0f; // Đẩy xuống dưới
        }

        // Biên dưới
        float distBottom = worldH - (y + radius);
        if (distBottom < safeZone) {
            float t = 1f - distBottom / safeZone;
            t = Math.max(0f, Math.min(1f, t));
            force.y -= t * t * 3.0f; // Đẩy lên trên
        }

        return force;
    }
}

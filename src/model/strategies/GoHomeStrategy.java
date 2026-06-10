package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.structures.House;
import model.world.World;

import java.util.List;

/**
 * GoHomeStrategy — Chiến lược về nhà ban đêm (đơn giản hóa).
 *
 * Cơ chế:
 *   1. Mỗi frame, quét các thực thể lân cận để tìm nhà đang chạm tới.
 *   2. Nếu chạm nhà → gọi {@link Human#goSleep(House)} → human biến mất ngay.
 *   3. Nếu chưa chạm → đi thẳng về phía nhà gần nhất bằng steering đơn giản.
 *   4. Ban ngày → {@link #shouldInterrupt} trả true → StrategySelector chuyển sang WANDER.
 *
 * Không dùng PathNavigator phức tạp — chỉ cần Vector2 steering.
 * Con người đi lại tự nhiên, hễ chạm vào bất kỳ nhà nào là biến mất.
 */
public class GoHomeStrategy implements IStrategy {

    private static final float WALK_SPEED_MULTIPLIER = 0.70f;

    /** Nhà mục tiêu để hướng đến (cập nhật định kỳ). */
    private House targetHouse = null;
    private float retargetTimer = 0f;
    private static final float RETARGET_INTERVAL = 3.0f; // Tìm lại nhà mỗi 3 giây

    // =========================================================
    // EXECUTE
    // =========================================================

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Human) || world == null) return;
        Human human = (Human) owner;

        // Đang ngủ trong nhà → giữ nguyên
        if (human.isSleeping()) {
            executeSleep(human, deltaTime);
            return;
        }

        // Quét nhà đang chạm ngay lập tức → biến mất
        if (tryEnterTouchedHouse(human, world)) {
            return;
        }

        // Chưa chạm → đi về phía nhà gần nhất
        moveTowardHome(human, world, deltaTime);
    }

    // =========================================================
    // ĐANG NGỦ TRONG NHÀ
    // =========================================================

    private void executeSleep(Human human, float deltaTime) {
        human.setSpeed(0);
        human.setActionState("sleep");

        // Tiêu hao cực ít khi ngủ
        human.setHunger(Math.max(0, human.getHunger()
                - human.getHungerDecayRate() * deltaTime * 0.05));
        human.setThirst(Math.max(0, human.getThirst()
                - human.getThirstDecayRate() * deltaTime * 0.05));

        // Hồi máu dần khi ngủ
        human.heal(2.0 * deltaTime);
    }

    // =========================================================
    // PHÁT HIỆN CHẠM NHÀ — BIẾN MẤT NGAY
    // =========================================================

    /**
     * Quét tất cả nhà trong tầm chạm của Human.
     * Nếu tìm thấy bất kỳ nhà nào đang chạm → gọi goSleep() → biến mất.
     *
     * @return true nếu đã ngủ thành công
     */
    private boolean tryEnterTouchedHouse(Human human, World world) {
        if (world.getSpatialGrid() == null) return false;

        // Bán kính quét = kích thước human + kích thước nhà lớn nhất có thể
        float scanRadius = human.getSize() + 80f;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(human.getPosition(), scanRadius);

        for (Entity entity : nearby) {
            if (!(entity instanceof House) || !entity.isAlive()) continue;
            House house = (House) entity;

            // Ưu tiên nhà trong homeArea của human, nhưng bất kỳ nhà nào chạm được đều OK
            if (human.goSleep(house)) {
                targetHouse = null; // Reset target sau khi đã vào nhà
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // ĐI VỀ PHÍA NHÀ GẦN NHẤT
    // =========================================================

    private void moveTowardHome(Human human, World world, float deltaTime) {
        // Tìm lại nhà mục tiêu định kỳ
        retargetTimer -= deltaTime;
        if (retargetTimer <= 0 || targetHouse == null || !targetHouse.isAlive()) {
            targetHouse = findBestHouse(human, world);
            retargetTimer = RETARGET_INTERVAL;
        }

        if (targetHouse == null) {
            // Không có nhà → đứng yên chờ sáng
            human.setSpeed(0);
            human.setActionState("idle");
            return;
        }

        // Steering đơn giản: đi thẳng về phía nhà
        Vector2 dir = targetHouse.getPosition().copy().subtract(human.getPosition());
        float dist = dir.length();

        if (dist < 1f) {
            human.setSpeed(0);
            human.setActionState("idle");
            return;
        }

        dir.normalize();
        human.setActionState("walk");
        human.setSpeed(human.getBaseSpeed() * WALK_SPEED_MULTIPLIER);

        // Tránh va chạm với entity cứng
        Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(human, world, dir);
        Vector2 finalDir = dir.copy();
        if (avoidance.lengthSquared() > 0) {
            finalDir.add(avoidance.scale(0.8f));
            if (finalDir.lengthSquared() > 0) finalDir.normalize();
        }

        if (finalDir.x > 0) human.setFacingRight(true);
        else if (finalDir.x < 0) human.setFacingRight(false);

        human.move(finalDir, deltaTime);
    }

    // =========================================================
    // TÌM NHÀ TỐT NHẤT
    // =========================================================

    /**
     * Ưu tiên nhà trong homeArea của human → fallback nhà gần nhất toàn bản đồ.
     */
    private House findBestHouse(Human human, World world) {
        if (world.getSettlementManager() == null) return null;

        // Thử nhà trong homeArea trước
        House nearestInHome = world.getSettlementManager().findNearestHouse(human.getHomeCenter());
        if (nearestInHome != null && human.isInHomeArea(nearestInHome.getPosition())) {
            return nearestInHome;
        }

        // Fallback: nhà gần nhất bất kỳ
        return world.getSettlementManager().findNearestHouse(human.getPosition());
    }

    // =========================================================
    // INTERRUPT / METADATA
    // =========================================================

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        if (!(owner instanceof Human)) return true;
        Human human = (Human) owner;

        boolean isNight = world != null &&
                (world.getTimeOfDay() >= 18.0f || world.getTimeOfDay() <= 5.0f);

        // Nếu đang ngủ trong nhà → chỉ ngắt khi trời sáng
        if (human.isSleeping()) {
            return !isNight;
        }

        // Nếu đang đi về → ngắt khi trời sáng
        return !isNight;
    }

    @Override
    public int getPriority() {
        return 85;
    }

    @Override
    public String getName() {
        return "GoHome";
    }

    @Override
    public Vector2 getTarget() {
        return targetHouse != null ? targetHouse.getPosition() : null;
    }

    @Override
    public List<Vector2> getPath() {
        return null; // Không dùng PathNavigator, không có path để hiển thị
    }
}

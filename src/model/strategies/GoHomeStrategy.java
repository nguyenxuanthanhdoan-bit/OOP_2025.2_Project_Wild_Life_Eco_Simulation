package model.strategies;

import core.Vector2;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.structures.House;
import model.world.World;

import java.util.List;

/**
 * GoHomeStrategy — Về nhà ban đêm dùng A* (PathNavigator).
 *
 * Lý do dùng A* thay vì vector thẳng:
 *   - Tránh kẹt vào House, giếng, decorative (lỗi tính đúng, không chỉ thẩm mỹ).
 *   - Human thức dậy xuất hiện tại điểm hợp lệ cạnh cửa, không bị kẹt trong tâm House.
 */
public class GoHomeStrategy implements IStrategy {

    private static final float WALK_SPEED_MULTIPLIER = 0.70f;

    private House targetHouse = null;
    private float retargetTimer = 0f;
    private static final float RETARGET_INTERVAL = 3.0f;

    private final PathNavigator navigator = new PathNavigator();

    // =========================================================
    // EXECUTE
    // =========================================================

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Human) || world == null) return;
        Human human = (Human) owner;

        if (human.isSleeping()) {
            executeSleep(human, deltaTime);
            return;
        }

        if (tryEnterNearbyHouse(human, world)) return;

        moveTowardHome(human, world, deltaTime);
    }

    // =========================================================
    // ĐANG NGỦ TRONG NHÀ
    // =========================================================

    private void executeSleep(Human human, float deltaTime) {
        human.setSpeed(0);
        human.setActionState("sleep");
        human.setHunger(Math.max(0, human.getHunger() - human.getHungerDecayRate() * deltaTime * 0.05));
        human.setThirst(Math.max(0, human.getThirst() - human.getThirstDecayRate() * deltaTime * 0.05));
        human.heal(2.0 * deltaTime);
    }

    // =========================================================
    // PHÁT HIỆN CHẠM NHÀ
    // =========================================================

    private boolean tryEnterNearbyHouse(Human human, World world) {
        if (world.getSpatialGrid() == null) return false;
        float scanRadius = human.getSize() + 80f;
        List<model.entity.Entity> nearby = world.getSpatialGrid().getNeighbors(human.getPosition(), scanRadius);

        for (model.entity.Entity entity : nearby) {
            if (!(entity instanceof House) || !entity.isAlive()) continue;
            House house = (House) entity;
            if (human.getHomeSettlement() != null
                    && !human.getHomeSettlement().containsHouse(house)) {
                continue;
            }
            if (human.goSleep(house)) {
                targetHouse = null;
                navigator.clear();
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // ĐI VỀ NHÀ — DÙNG A*
    // =========================================================

    private void moveTowardHome(Human human, World world, float deltaTime) {
        retargetTimer -= deltaTime;
        if (retargetTimer <= 0 || targetHouse == null || !targetHouse.isAlive()) {
            targetHouse = findBestHouse(human, world);
            retargetTimer = RETARGET_INTERVAL;
            navigator.clear();
        }

        if (targetHouse == null) {
            human.setSpeed(0);
            human.setActionState("idle");
            return;
        }

        // Tìm điểm interaction hợp lệ cạnh nhà (tránh đứng vào tâm solid)
        float enterRange = human.getSize() / 2 + targetHouse.getSize() / 2 + 12.0f;
        Vector2 target = PathNavigator.findInteractionPoint(human, world, targetHouse, enterRange);

        human.setActionState("walk");
        human.setSpeed(human.getBaseSpeed() * WALK_SPEED_MULTIPLIER);

        navigator.moveTo(human, world, target, deltaTime,
                8.0f, 0.5f, MovementContext.SEEKING_STRUCTURE);

        if (navigator.isBlocked()) {
            // A* bị chặn → thử nhà khác
            targetHouse = null;
            retargetTimer = 0;
            navigator.clear();
        }
    }

    private House findBestHouse(Human human, World world) {
        if (human.getHomeSettlement() != null) {
            return human.getHomeSettlement().findNearestAvailableHouse(human.getPosition());
        }
        if (world.getSettlementManager() == null) return null;
        House nearestInHome = world.getSettlementManager().findNearestHouse(human.getHomeCenter());
        if (nearestInHome != null && human.isInHomeArea(nearestInHome.getPosition())) {
            return nearestInHome;
        }
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
        if (human.isSleeping()) return !isNight;
        return !isNight;
    }

    @Override public int getPriority() { return 85; }
    @Override public String getName()  { return "GoHome"; }

    @Override
    public Vector2 getTarget() {
        return targetHouse != null ? targetHouse.getPosition() : null;
    }

    @Override
    public List<Vector2> getPath() {
        return navigator.getPath();
    }
}

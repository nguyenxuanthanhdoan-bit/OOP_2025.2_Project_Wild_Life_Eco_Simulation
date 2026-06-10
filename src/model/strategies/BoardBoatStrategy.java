package model.strategies;

import model.living_beings.Animal;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.structures.Boat;
import model.structures.FoodStorage;
import model.world.World;
import core.Vector2;

public class BoardBoatStrategy implements IStrategy {
    private Boat targetBoat;
    private boolean boarded = false;
    private boolean finished = false;
    private boolean onSea = false; // đang ngoài biển → non-interruptible
    private final PathNavigator navigator = new PathNavigator();
    private Vector2 boardingPoint;
    private FoodStorage targetStorage;

    public BoardBoatStrategy(Boat boat) {
        this.targetBoat = boat;
    }

    @Override
    public void onExit(LivingBeing owner, World world) {
        if (owner instanceof Human && targetBoat != null) {
            targetBoat.unboard((Human) owner);
        }
        navigator.clear();
    }

    @Override
    public boolean isInNonInterruptiblePhase() {
        return onSea; // đang ngoài biển → không được ngắt
    }

    @Override
    public boolean isCommittedTask() {
        return !finished;
    }

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Human)) return;
        Human human = (Human) owner;

        if (targetBoat == null || targetBoat.getWorld() == null) {
            finished = true;
            return;
        }

        if (!boarded) {
            if (!targetBoat.isReservedBy(human) && !targetBoat.reserveSeat(human)) {
                finished = true;
                return;
            }
            if (boardingPoint == null || !world.isValidPositionFor(human, boardingPoint)) {
                boardingPoint = findSafeShorePoint(human, world);
            }
            if (boardingPoint == null) {
                finished = true;
                return;
            }

            float dist = human.getPosition().distanceTo(boardingPoint);
            if (dist <= 12.0f) {
                if (targetBoat.board(human)) {
                    boarded = true;
                    onSea = true;
                    human.setSpeed(0);
                } else {
                    finished = true; // thuyền đã chạy hoặc đầy
                }
            } else {
                // Đi tới thuyền bằng PathNavigator thay vì vector thẳng
                human.setActionState("walk");
                human.setSpeed(human.getBaseSpeed());
                navigator.moveTo((Animal) human, world, boardingPoint, deltaTime,
                        8.0f, 1.0f, MovementContext.SEEKING_STRUCTURE);
                if (navigator.isBlocked()) {
                    finished = true;
                }
            }
        } else {
            // Đã lên thuyền, chờ cập bến
            if (targetBoat.getState() == Boat.BoatState.DOCKED) {
                targetBoat.unboard(human);
                onSea = false;

                Vector2 disembarkPoint = findSafeShorePoint(human, world);
                if (disembarkPoint != null) {
                    human.setPosition(disembarkPoint);
                }
                targetStorage = human.getHomeSettlement() == null ? null
                        : human.getHomeSettlement().findNearestFoodStorage(human.getPosition(), false);
                navigator.clear();
            } else {
                return;
            }
        }

        if (boarded && !onSea) {
            depositCatch(human, world, deltaTime);
        }
    }

    private void depositCatch(Human human, World world, float deltaTime) {
        if (!human.hasCarriedFood() || targetStorage == null || !targetStorage.isAlive()) {
            finished = true;
            return;
        }

        float depositRange = human.getSize() / 2 + targetStorage.getSize() / 2 + 20.0f;
        if (human.getPosition().distanceTo(targetStorage.getPosition()) <= depositRange) {
            human.setSpeed(0);
            human.setActionState("idle");
            human.depositFood(targetStorage);
            finished = true;
            return;
        }

        human.setActionState("walk");
        human.setSpeed(human.getBaseSpeed());
        Vector2 target = PathNavigator.findInteractionPoint(human, world, targetStorage, depositRange);
        navigator.moveTo(human, world, target, deltaTime,
                8.0f, 1.0f, MovementContext.SEEKING_STRUCTURE);
        if (navigator.isBlocked()) finished = true;
    }

    private Vector2 findSafeShorePoint(Human human, World world) {
        Vector2 center = targetBoat.getDockPosition();
        Vector2 towardHome = human.getHomeCenter().copy().subtract(center);
        if (towardHome.lengthSquared() <= 0.001f) towardHome.set(1, 0);
        towardHome.normalize();

        float[] radii = {64.0f, 80.0f, 104.0f, 128.0f, 160.0f};
        float[] angles = {0, 30, -30, 60, -60, 90, -90, 135, -135, 180};
        for (float radius : radii) {
            for (float angle : angles) {
                Vector2 direction = rotate(towardHome, angle);
                Vector2 candidate = center.copy().add(direction.scale(radius));
                if (world.isValidPositionFor(human, candidate)) return candidate;
            }
        }
        return null;
    }

    private Vector2 rotate(Vector2 vector, float degrees) {
        double radians = Math.toRadians(degrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector2(vector.x * cos - vector.y * sin,
                vector.x * sin + vector.y * cos);
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return finished || targetBoat == null;
    }

    @Override public int getPriority() { return 60; }
    @Override public String getName()  { return "BoardBoat"; }

    @Override
    public Vector2 getTarget() {
        return targetBoat != null ? targetBoat.getPosition() : null;
    }

    @Override
    public java.util.List<Vector2> getPath() {
        return navigator.getPath();
    }
}

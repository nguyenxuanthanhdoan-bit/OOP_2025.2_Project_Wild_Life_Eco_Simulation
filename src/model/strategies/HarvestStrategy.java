package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Human;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.structures.FoodStorage;
import model.structures.GardenBed;
import model.world.World;

/**
 * AI Strategy cho Human: Đi đến chậu cây chín và thu hoạch.
 *
 * Reservation lifecycle:
 *   onEnter → xác nhận reservation thuộc đúng Human
 *   onExit  → release reservation nếu chưa harvest xong
 */
public class HarvestStrategy implements IStrategy {

    private GardenBed targetBed;
    private final PathNavigator navigator = new PathNavigator();
    private float harvestTimer = 0f;
    private boolean harvested = false;
    private boolean harvesting = false; // đang đứng tại chỗ thu hoạch
    private boolean finished = false;
    private FoodStorage targetStorage;
    private static final float HARVEST_DURATION = 3.0f;

    public HarvestStrategy(GardenBed targetBed) {
        this.targetBed = targetBed;
        // KHÔNG reserve ở đây — để onEnter làm
    }

    @Override
    public void onEnter(LivingBeing owner, World world) {
        if (!(owner instanceof Human) || targetBed == null || !targetBed.reserve(owner)) {
            finished = true;
        }
    }

    @Override
    public void onExit(LivingBeing owner, World world) {
        if (targetBed != null && !harvested) targetBed.releaseReservation(owner);
        harvesting = false;
        navigator.clear();
    }

    @Override
    public boolean isCommittedTask() {
        return !finished;
    }

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Human) || world == null || finished) return;
        Human human = (Human) owner;

        if (harvested) {
            depositHarvest(human, world, deltaTime);
            return;
        }

        if (targetBed == null || !targetBed.isReservedBy(human)) {
            finished = true;
            return;
        }

        if (!targetBed.isMature()) {
            targetBed.releaseReservation(human);
            targetBed = null;
            finished = true;
            return;
        }

        float dist = owner.getPosition().distanceTo(targetBed.getPosition());

        if (dist <= 60.0f) {
            harvesting = true;
            human.setActionState("idle");
            human.setSpeed(0);

            harvestTimer += deltaTime;
            if (harvestTimer >= HARVEST_DURATION) {
                human.addCarriedFood(targetBed.harvest(human));
                harvested = true;
                harvesting = false;
                targetStorage = human.getHomeSettlement() == null ? null
                        : human.getHomeSettlement().findNearestFoodStorage(human.getPosition(), false);
                navigator.clear();
            }
        } else {
            harvesting = false;
            human.setActionState("walk");
            human.setSpeed(human.getBaseSpeed() * 0.5f);
            navigator.moveTo((model.living_beings.Animal) owner, world, targetBed.getPosition(), deltaTime, 18.0f, 1.5f);

            if (navigator.isBlocked()) {
                targetBed.releaseReservation(human);
                targetBed = null;
                finished = true;
                navigator.clear();
            }
        }
    }

    private void depositHarvest(Human human, World world, float deltaTime) {
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
        navigator.moveTo(human, world, target, deltaTime, 8.0f, 1.0f, MovementContext.SEEKING_STRUCTURE);
        if (navigator.isBlocked()) finished = true;
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        // shouldInterrupt không có side effect — chỉ quan sát trạng thái
        return finished;
    }

    @Override public int getPriority() { return 70; }
    @Override public String getName()  { return "Harvest"; }

    @Override
    public Vector2 getTarget() {
        return targetBed != null ? targetBed.getPosition() : null;
    }

    @Override
    public java.util.List<Vector2> getPath() {
        return navigator.getPath();
    }
}

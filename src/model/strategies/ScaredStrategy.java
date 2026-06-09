package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.navigation.PathNavigator;
import model.world.World;
import model.structures.Bush;
import model.entity.Entity;
import java.util.List;
import java.util.ArrayList;

/**
 * ScaredStrategy — Chiến lược bỏ chạy / ẩn nấp.
 * Trách nhiệm DUY NHẤT: Giúp con vật trốn thoát kẻ săn mồi.
 * Việc chuyển sang Strategy khác (Forage, Mating...) là trách nhiệm
 * của Animal.decideActiveStrategy(), không phải của class này.
 */
public class ScaredStrategy implements IStrategy {
    private static final float RUN_SPEED_MULTIPLIER = 1.8f;
    private final PathNavigator shelterNavigator = new PathNavigator();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;
        if (!ownerAnimal.canUseStrategy(ScaredStrategy.class)) return;
        if (world == null || world.getSpatialGrid() == null) return;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(
                ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());

        List<Animal> predators = new ArrayList<>();
        List<Bush> bushes = new ArrayList<>();

        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                Animal other = (Animal) neighbor;
                if (ownerAnimal.isThreatenedBy(other)) {
                    predators.add(other);
                }
            } else if (ownerAnimal.getProfile().canHide() && neighbor instanceof Bush) {
                Bush bush = (Bush) neighbor;
                if (!bush.isOccupied() || bush == ownerAnimal.getHiddenInBush()) {
                    bushes.add(bush);
                }
            }
        }

        // Không có kẻ thù → Animal.decideActiveStrategy() sẽ thoát Strategy này
        if (predators.isEmpty()) {
            shelterNavigator.clear();
            ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            ownerAnimal.setActionState("idle");
            return;
        }

        // Có kẻ thù → Tìm bụi cỏ tốt nhất để trốn
        Bush bestBush = null;
        float maxScore = -999999.0f;

        for (Bush bush : bushes) {
            if (bush.isOccupied() && bush != ownerAnimal.getHiddenInBush()) continue;

            float distToPredators = 0;
            for (Animal predator : predators) {
                distToPredators += bush.getPosition().distanceTo(predator.getPosition());
            }
            float distToOwner = bush.getPosition().distanceTo(ownerAnimal.getPosition());
            // Ưu tiên bụi gần mình + xa kẻ thù
            float score = distToPredators - distToOwner * 2.0f;

            if (score > maxScore) {
                maxScore = score;
                bestBush = bush;
            }
        }

        if (bestBush != null) {
            float distToBush = ownerAnimal.getPosition().distanceTo(bestBush.getPosition());
            if (distToBush <= bestBush.getRadius()) {
                // Đã vào trong bụi → ẩn nấp
                if (!ownerAnimal.isHidden()) ownerAnimal.hideInBush(bestBush);
                shelterNavigator.clear();
                ownerAnimal.setActionState("idle");
                ownerAnimal.setSpeed(0);
            } else {
                // Chạy tới bụi
                if (ownerAnimal.isHidden()) ownerAnimal.exitBush();
                ownerAnimal.setActionState("run");
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * RUN_SPEED_MULTIPLIER);

                shelterNavigator.moveTo(ownerAnimal, world, bestBush.getPosition(), deltaTime,
                        bestBush.getRadius(), 0.5f);
                if (shelterNavigator.isBlocked()) {
                    fleeFromPredators(ownerAnimal, predators, world, deltaTime);
                }
            }
        } else {
            // Không có bụi → bỏ chạy ngược hướng kẻ thù
            fleeFromPredators(ownerAnimal, predators, world, deltaTime);
        }
    }

    private void fleeFromPredators(Animal ownerAnimal, List<Animal> predators, World world, float deltaTime) {
        shelterNavigator.clear();
        if (ownerAnimal.isHidden()) ownerAnimal.exitBush();
        ownerAnimal.setActionState("run");
        ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * RUN_SPEED_MULTIPLIER);

        Vector2 fleeDir = new Vector2();
        for (Animal predator : predators) {
            Vector2 diff = ownerAnimal.getPosition().copy().subtract(predator.getPosition());
            if (diff.lengthSquared() > 0) {
                diff.normalize();
                fleeDir.add(diff);
            }
        }
        if (fleeDir.lengthSquared() > 0) fleeDir.normalize();
        else fleeDir.set(1, 0);

        Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(ownerAnimal, world, fleeDir);
        Vector2 finalDir = fleeDir.copy();
        if (avoidance.lengthSquared() > 0) {
            float avoidMag = avoidance.length();
            if (avoidMag > 1.5f) finalDir = avoidance.copy();
            else finalDir.add(avoidance.scale(2.0f));
            if (finalDir.lengthSquared() > 0) finalDir.normalize();
        }

        if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
        else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
        ownerAnimal.move(finalDir, deltaTime);
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) { return false; }

    @Override
    public int getPriority() { return 4; }

    @Override
    public String getName() { return "Scared"; }

    @Override
    public core.Vector2 getTarget() {
        return shelterNavigator.getLastTarget();
    }

    @Override
    public java.util.List<core.Vector2> getPath() {
        return shelterNavigator.getPath();
    }
}

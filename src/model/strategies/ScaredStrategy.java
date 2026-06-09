package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.living_beings.Human;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.world.World;
import model.structures.Bush;
import model.structures.House;
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
        List<House> houses = new ArrayList<>();
        Human human = ownerAnimal instanceof Human ? (Human) ownerAnimal : null;

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
            } else if (human != null && human.isVillager() && neighbor instanceof House) {
                House house = (House) neighbor;
                if ((house.hasSpace() || house == human.getHiddenInHouse()) && human.isInHomeArea(house.getPosition())) {
                    houses.add(house);
                }
            }
        }

        // Không có kẻ thù → Animal.decideActiveStrategy() sẽ thoát Strategy này
        if (predators.isEmpty()) {
            if (human != null && human.getHiddenInHouse() != null) human.exitHouse();
            shelterNavigator.clear();
            ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            ownerAnimal.setActionState("idle");
            return;
        }

        if (human != null && human.isVillager()) {
            if (handleHouseShelter(human, houses, predators, world, deltaTime)) {
                return;
            }
            fleeFromPredators(ownerAnimal, predators, world, deltaTime);
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

    private boolean handleHouseShelter(Human human, List<House> houses, List<Animal> predators,
                                       World world, float deltaTime) {
        House bestHouse = null;
        float maxScore = -999999.0f;

        for (House house : houses) {
            if (!house.hasSpace() && house != human.getHiddenInHouse()) continue;

            float distToPredators = 0;
            for (Animal predator : predators) {
                distToPredators += house.getPosition().distanceTo(predator.getPosition());
            }
            float distToHuman = house.getPosition().distanceTo(human.getPosition());
            float score = distToPredators - distToHuman * 2.0f;

            if (score > maxScore) {
                maxScore = score;
                bestHouse = house;
            }
        }

        if (bestHouse == null) return false;

        float enterRange = human.getSize() / 2 + bestHouse.getSize() / 2 + 12.0f;
        if (human.getPosition().distanceTo(bestHouse.getPosition()) <= enterRange) {
            human.enterHouse(bestHouse);
            shelterNavigator.clear();
            return true;
        }

        if (human.getHiddenInHouse() != null) human.exitHouse();
        human.setActionState("run");
        human.setSpeed(human.getBaseSpeed() * RUN_SPEED_MULTIPLIER);
        Vector2 target = PathNavigator.findInteractionPoint(human, world, bestHouse, enterRange);
        shelterNavigator.moveTo(human, world, target, deltaTime, 8.0f, 0.5f, MovementContext.SEEKING_STRUCTURE);
        return !shelterNavigator.isBlocked();
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
}

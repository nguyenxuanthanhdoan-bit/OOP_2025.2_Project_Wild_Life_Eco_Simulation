package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.world.World;
import model.structures.Bush;
import model.entity.Entity;
import java.util.List;
import java.util.ArrayList;

public class ScaredStrategy implements IStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private static final double RUN_COST_MULTIPLIER = 3.0;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        if (world == null || world.getSpatialGrid() == null) return;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());

        List<Animal> predators = new ArrayList<>();
        List<Bush> bushes = new ArrayList<>();

        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                Animal other = (Animal) neighbor;
                if (other.isAliveState() && other.getDietType() == DietType.CARNIVORE && other.getSize() > ownerAnimal.getSize()) {
                    predators.add(other);
                } else if (other.isAliveState() && other.getDietType() == DietType.CARNIVORE && ownerAnimal.getDietType() == DietType.HERBIVORE) {
                    predators.add(other);
                }
            } else if (neighbor instanceof Bush) {
                Bush bush = (Bush) neighbor;
                if (!bush.isOccupied() || (ownerAnimal.getPosition().distanceTo(bush.getPosition()) <= bush.getRadius())) {
                    bushes.add(bush);
                }
            }
        }

        if (predators.isEmpty()) {
            if (ownerAnimal.isHidden()) {
                ownerAnimal.exitBush();
            }
            if (ownerAnimal.getSpeed() != ownerAnimal.getBaseSpeed()) {
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            }
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Không tiêu hao stamina nếu đang ẩn nấp
        if (!ownerAnimal.isHidden()) {
            double extraHunger = ownerAnimal.getHungerDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
            double extraThirst = ownerAnimal.getThirstDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
            ownerAnimal.setHunger(ownerAnimal.getHunger() - extraHunger);
            ownerAnimal.setThirst(ownerAnimal.getThirst() - extraThirst);
        }

        Bush bestBush = null;
        float maxDistToPredators = -1.0f;

        for (Bush bush : bushes) {
            float distToPredatorsForBush = 0;
            for (Animal predator : predators) {
                distToPredatorsForBush += bush.getPosition().distanceTo(predator.getPosition());
            }
            if (distToPredatorsForBush > maxDistToPredators) {
                maxDistToPredators = distToPredatorsForBush;
                bestBush = bush;
            }
        }

        if (bestBush != null) {
            float distToBush = ownerAnimal.getPosition().distanceTo(bestBush.getPosition());
            if (distToBush <= bestBush.getRadius()) {
                if (!ownerAnimal.isHidden()) {
                    ownerAnimal.hideInBush(bestBush);
                }
                ownerAnimal.setActionState("idle"); 
                ownerAnimal.setSpeed(0); 
            } else {
                if (ownerAnimal.isHidden()) {
                    ownerAnimal.exitBush();
                }
                ownerAnimal.setActionState("run");
                ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.5f));
                
                Vector2 dirToBush = bestBush.getPosition().copy().subtract(ownerAnimal.getPosition());
                if (dirToBush.lengthSquared() > 0) dirToBush.normalize();

                Vector2 finalDir = dirToBush.copy();
                Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(ownerAnimal, world);
                if (avoidance.lengthSquared() > 0) {
                    finalDir.add(avoidance);
                    if (finalDir.lengthSquared() > 0) finalDir.normalize();
                }

                if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
                else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
                
                ownerAnimal.move(finalDir, deltaTime);
            }
        } else {
            if (ownerAnimal.isHidden()) {
                ownerAnimal.exitBush();
            }
            ownerAnimal.setActionState("run");
            ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.5f));

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

            Vector2 finalDir = fleeDir.copy();
            Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(ownerAnimal, world);
            if (avoidance.lengthSquared() > 0) {
                finalDir.add(avoidance);
                if (finalDir.lengthSquared() > 0) finalDir.normalize();
            }

            if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
            else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
            
            ownerAnimal.move(finalDir, deltaTime);
        }
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public String getName() {
        return "Scared";
    }
}

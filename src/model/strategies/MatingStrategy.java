package model.strategies;

import core.Vector2;
import core.GameConfig;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.navigation.PathNavigator;
import model.world.World;
import model.world.WorldEventType;
import model.entity.Entity;
import java.util.List;

public class MatingStrategy extends PassiveStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private final PathNavigator mateNavigator = new PathNavigator();
    private final GameConfig config = GameConfig.getInstance();
    private Animal targetMate = null;
    private float matingTimer = 0.0f;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        // Check if ecosystem reached limit
        if (getAnimalCount(world) >= config.MAX_ANIMAL_POPULATION) {
            mateNavigator.clear();
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Check self mating conditions
        if (!ownerAnimal.canReproduce()) {
            mateNavigator.clear();
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Validate current mate
        if (targetMate != null) {
            if (!ownerAnimal.canMateWith(targetMate) || ownerAnimal.getPosition().distanceTo(targetMate.getPosition()) > ownerAnimal.getVisionRange()) {
                targetMate = null;
                mateNavigator.clear();
            }
        }

        // Find mate if none
        if (targetMate == null && world != null && world.getSpatialGrid() != null) {
            List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());
            float closestDist = Float.MAX_VALUE;

            for (Entity neighbor : neighbors) {
                if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                    Animal other = (Animal) neighbor;
                    if (ownerAnimal.canMateWith(other)) {
                        float dist = ownerAnimal.getPosition().distanceTo(other.getPosition());
                        if (dist < closestDist) {
                            closestDist = dist;
                            targetMate = other;
                        }
                    }
                }
            }
        }

        if (targetMate != null) {
            Vector2 dirToMate = targetMate.getPosition().copy().subtract(ownerAnimal.getPosition());
            float distToMate = dirToMate.length();

            float mateRange = ownerAnimal.getSize() / 2 + targetMate.getSize() / 2 + 10.0f;
            if (distToMate <= mateRange) {
                ownerAnimal.setActionState("idle");
                ownerAnimal.setSpeed(0);

                matingTimer += deltaTime;
                if (matingTimer >= config.MATING_DURATION_SECONDS) {
                    // Mate!
                    Animal child = ownerAnimal.reproduce();
                    if (child != null) {
                        world.addEntity(child);
                        world.publishEvent(WorldEventType.ANIMAL_BORN, child, ownerAnimal.getSpeciesName());
                        // Consume energy for both parents
                        ownerAnimal.setHunger(Math.max(0, ownerAnimal.getHunger() - config.REPRODUCTION_ENERGY_COST));
                        ownerAnimal.setThirst(Math.max(0, ownerAnimal.getThirst() - config.REPRODUCTION_ENERGY_COST));
                        targetMate.setHunger(Math.max(0, targetMate.getHunger() - config.REPRODUCTION_ENERGY_COST));
                        targetMate.setThirst(Math.max(0, targetMate.getThirst() - config.REPRODUCTION_ENERGY_COST));
                        
                        ownerAnimal.startReproductionCooldown();
                        targetMate.startReproductionCooldown();
                        targetMate = null;
                        mateNavigator.clear();
                    }
                    matingTimer = 0.0f;
                }
            } else {
                matingTimer = 0.0f; // Reset nếu bị tách ra
                ownerAnimal.setActionState("run"); // or walk
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
                mateNavigator.moveTo(ownerAnimal, world, targetMate.getPosition(), deltaTime, mateRange, 1.0f);
                if (mateNavigator.isBlocked()) {
                    targetMate = null;
                    ownerAnimal.setActionState("idle");
                }
            }
        } else {
            mateNavigator.clear();
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
        }
    }

    private int getAnimalCount(World world) {
        if (world == null || world.getEntities() == null) return 0;
        int count = 0;
        for (Entity e : world.getEntities()) {
            if (e instanceof Animal && e.isAlive()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public String getName() {
        return "Mating";
    }

    @Override
    public core.Vector2 getTarget() {
        if (targetMate != null) return targetMate.getPosition();
        return wanderDelegate.getTarget();
    }

    @Override
    public java.util.List<core.Vector2> getPath() {
        if (targetMate != null) return mateNavigator.getPath();
        return wanderDelegate.getPath();
    }
}

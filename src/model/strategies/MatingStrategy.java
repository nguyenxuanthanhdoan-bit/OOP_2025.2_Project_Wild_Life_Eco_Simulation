package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.world.World;
import model.entity.Entity;
import java.util.List;

public class MatingStrategy extends PassiveStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private static final int MAX_POPULATION = 150;
    private Animal targetMate = null;
    private float cooldownTimer = 0.0f;
    private float matingTimer = 0.0f;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        if (cooldownTimer > 0) {
            cooldownTimer -= deltaTime;
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Check if ecosystem reached limit
        if (getAnimalCount(world) >= MAX_POPULATION) {
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Check self mating conditions
        if (!ownerAnimal.canReproduce()) {
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Validate current mate
        if (targetMate != null) {
            if (!targetMate.isAliveState() || !targetMate.canReproduce() || ownerAnimal.getPosition().distanceTo(targetMate.getPosition()) > ownerAnimal.getVisionRange()) {
                targetMate = null;
            }
        }

        // Find mate if none
        if (targetMate == null && world != null && world.getSpatialGrid() != null) {
            List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());
            float closestDist = Float.MAX_VALUE;

            for (Entity neighbor : neighbors) {
                if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                    Animal other = (Animal) neighbor;
                    if (other.isAliveState() && other.getSpeciesName().equals(ownerAnimal.getSpeciesName()) && other.canReproduce()) {
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
                if (matingTimer >= 2.0f) {
                    // Mate!
                    Animal child = ownerAnimal.reproduce();
                    if (child != null) {
                        world.addEntity(child);
                        // Consume energy for both parents
                        ownerAnimal.setHunger(Math.max(0, ownerAnimal.getHunger() - 50));
                        ownerAnimal.setThirst(Math.max(0, ownerAnimal.getThirst() - 50));
                        targetMate.setHunger(Math.max(0, targetMate.getHunger() - 50));
                        targetMate.setThirst(Math.max(0, targetMate.getThirst() - 50));
                        
                        // Start cooldown
                        cooldownTimer = 60.0f; // 60 seconds cooldown
                        targetMate = null;
                    }
                    matingTimer = 0.0f;
                }
            } else {
                matingTimer = 0.0f; // Reset nếu bị tách ra
                // Move to mate
                Vector2 finalDir = dirToMate.copy();
                if (finalDir.lengthSquared() > 0) finalDir.normalize();
                
                Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(ownerAnimal, world);
                if (avoidance.lengthSquared() > 0) {
                    finalDir.add(avoidance);
                    if (finalDir.lengthSquared() > 0) finalDir.normalize();
                }

                if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
                else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
                
                ownerAnimal.setActionState("run"); // or walk
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
                ownerAnimal.move(finalDir, deltaTime);
            }
        } else {
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
}

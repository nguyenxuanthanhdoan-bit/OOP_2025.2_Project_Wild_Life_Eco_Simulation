package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.living_beings.LivingBeing;
import model.world.World;

import java.util.List;

public class SleepStrategy implements IStrategy {
    private Vector2 sleepTarget = null;
    private boolean isSleeping = false;
    private boolean hasSearched = false;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal animal = (Animal) owner;

        if (isSleeping) {
            animal.setSpeed(0);
            animal.getCurrentVelocity().set(0, 0);
            animal.setActionState("sleep");
            
            // Nghỉ ngơi tiêu hao rất ít năng lượng
            animal.setHunger(Math.max(0, animal.getHunger() - (animal.getHungerDecayRate() * deltaTime * 0.1f)));
            animal.setThirst(Math.max(0, animal.getThirst() - (animal.getThirstDecayRate() * deltaTime * 0.1f)));
            return;
        }

        if (!hasSearched && sleepTarget == null) {
            sleepTarget = findBestSleepSpot(animal, world);
            hasSearched = true;
        }

        if (sleepTarget != null) {
            float distSq = animal.getPosition().distanceSquared(sleepTarget);
            if (distSq < 225.0f) { // 15^2
                isSleeping = true;
            } else {
                animal.setActionState("walk");
                animal.setSpeed(animal.getBaseSpeed());
                Vector2 dir = sleepTarget.copy().subtract(animal.getPosition()).normalize();
                animal.getCurrentVelocity().set(dir.x * animal.getSpeed(), dir.y * animal.getSpeed());
            }
        } else {
            isSleeping = true; // Couldn't find a better spot, just sleep here
        }
    }

    private Vector2 findBestSleepSpot(Animal animal, World world) {
        boolean isHerbivore = animal.getDietType() == DietType.HERBIVORE;
        Vector2 bestSpot = animal.getPosition().copy();
        int bestScore = countTreesAt(world, bestSpot);

        if (isHerbivore && bestScore >= 3) return null;
        if (!isHerbivore && bestScore == 0) return null;

        for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4);
            float dx = (float) Math.cos(angle) * 300;
            float dy = (float) Math.sin(angle) * 300;
            Vector2 sample = animal.getPosition().copy().add(new Vector2(dx, dy));
            
            sample.x = Math.max(0, Math.min(world.getWidth(), sample.x));
            sample.y = Math.max(0, Math.min(world.getHeight(), sample.y));

            int score = countTreesAt(world, sample);
            if (isHerbivore) {
                if (score > bestScore) {
                    bestScore = score;
                    bestSpot = sample;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestSpot = sample;
                }
            }
        }
        
        if (bestSpot.distanceSquared(animal.getPosition()) < 1.0f) {
            return null;
        }
        return bestSpot;
    }

    private int countTreesAt(World world, Vector2 pos) {
        if (world == null || world.getSpatialGrid() == null) return 0;
        int count = 0;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(pos, 250);
        for (Entity e : nearby) {
            if (e instanceof model.plants.FruitTree || e instanceof model.plants.Grass) {
                count++;
            }
            if (e.getClass().getSimpleName().toLowerCase().contains("bush")) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        if (!(owner instanceof Animal)) return true;
        Animal animal = (Animal) owner;
        
        // Interrupt if attacked
        if (animal.hasDangerousThreats()) return true;
        
        // Interrupt if morning comes
        boolean isNight = (world.getTimeOfDay() >= 18.0f || world.getTimeOfDay() <= 5.0f);
        if (!isNight) return true;
        
        return false;
    }

    @Override
    public int getPriority() {
        return 90; // High priority, but less than ScaredStrategy (which might be 100)
    }

    @Override
    public String getName() {
        return "SleepStrategy";
    }
}

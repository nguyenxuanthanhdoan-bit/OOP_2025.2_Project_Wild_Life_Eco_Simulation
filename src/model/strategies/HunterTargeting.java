package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.items.Carcass;
import model.items.FoodSource;
import model.living_beings.Animal;
import model.living_beings.Human;
import model.structures.Bush;
import model.world.World;

import java.util.List;

public class HunterTargeting {
    private static final float BUSH_CONCEALMENT_QUERY_RADIUS = 96.0f;

    /**
     * Tìm mục tiêu săn mồi tốt nhất cho Animal (Thú ăn thịt/ăn tạp).
     */
    public static Entity findAnimalTarget(Animal hunter, World world) {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(hunter.getPosition(), (float) hunter.getVisionRange());
        float bestScore = -1.0f;
        Entity bestTarget = null;
        boolean isVeryHungry = hunter.getHunger() < hunter.getMaxHunger() * 0.6;

        for (Entity neighbor : neighbors) {
            if (!neighbor.isAlive() || neighbor == hunter) continue;
            if (hunter.isInDangerZone(neighbor.getPosition())) continue;

            float dist = hunter.getPosition().distanceTo(neighbor.getPosition());
            float score = 0;

            if (neighbor instanceof FoodSource) {
                FoodSource foodSource = (FoodSource) neighbor;
                if (!hunter.canEatFoodSource(foodSource)) continue;
                if (isFoodConcealedByBush(foodSource, world)) continue;

                score = 1000.0f - dist;
                if (isVeryHungry) score += 5000.0f;

            } else if (neighbor instanceof Animal) {
                Animal other = (Animal) neighbor;
                if (!hunter.canHunt(other)) continue;

                if (other.canUseStrategy(HunterStrategy.class) && !isVeryHungry) continue;

                score = 500.0f - dist;
                if (other.canUseStrategy(HunterStrategy.class)) score -= 300.0f;

                boolean isWeak = other.getHealth() < other.getMaxHealth() * 0.5 || other.getAge() > other.getMaxAge() * 0.8;
                if (isWeak) score += 2000.0f;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = neighbor;
            }
        }
        return bestTarget;
    }

    /**
     * Tìm mục tiêu săn mồi tốt nhất cho Human.
     */
    public static Entity findHumanHunterTarget(Human hunter, World world) {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(hunter.getPosition(), (float) hunter.getVisionRange());
        Entity bestTarget = null;
        float bestScore = -1.0f;

        for (Entity neighbor : neighbors) {
            if (!neighbor.isAlive() || neighbor == hunter) continue;

            float dist = hunter.getPosition().distanceTo(neighbor.getPosition());
            float score = 0.0f;

            if (neighbor instanceof FoodSource) {
                FoodSource food = (FoodSource) neighbor;
                if (!hunter.canEatFoodSource(food)) continue;
                score = 2400.0f - dist;
                if (neighbor instanceof Carcass) score += 350.0f;
            } else if (neighbor instanceof Animal) {
                Animal prey = (Animal) neighbor;
                if (!hunter.canHunt(prey)) continue;
                score = 900.0f - dist;
                if (prey.getHealth() < prey.getMaxHealth() * 0.5) score += 300.0f;
                if (prey.getEntityLevel() >= hunter.getEntityLevel()) score -= 500.0f;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = neighbor;
            }
        }
        return bestTarget;
    }

    /**
     * Chỉ tìm FoodSource cho Human Hunter.
     */
    public static Entity findHumanHunterFoodTarget(Human hunter, World world) {
        if (world == null || world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(hunter.getPosition(), (float) hunter.getVisionRange());
        Entity bestTarget = null;
        float bestScore = -1.0f;

        for (Entity neighbor : neighbors) {
            if (!(neighbor instanceof FoodSource) || !neighbor.isAlive()) continue;
            FoodSource food = (FoodSource) neighbor;
            if (!hunter.canEatFoodSource(food)) continue;

            float dist = hunter.getPosition().distanceTo(neighbor.getPosition());
            float score = 2400.0f - dist;
            if (neighbor instanceof Carcass) score += 350.0f;

            if (score > bestScore) {
                bestScore = score;
                bestTarget = neighbor;
            }
        }
        return bestTarget;
    }

    public static boolean isFoodConcealedByBush(Entity food, World world) {
        if (!(food instanceof FoodSource) || world == null || world.getSpatialGrid() == null) {
            return false;
        }
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(food.getPosition(), BUSH_CONCEALMENT_QUERY_RADIUS);
        for (Entity entity : nearby) {
            if (entity instanceof Bush && entity.isAlive() && ((Bush) entity).contains(food.getPosition())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTargetStillValid(Animal hunter, Entity target, World world) {
        if (target == null || !target.isAlive()) return false;
        float dist = hunter.getPosition().distanceTo(target.getPosition());
        if (dist > hunter.getVisionRange()) return false;
        if (hunter.isInDangerZone(target.getPosition())) return false;

        if (target instanceof FoodSource && isFoodConcealedByBush(target, world)) {
            return false;
        }
        if (target instanceof Animal) {
            Animal prey = (Animal) target;
            if (prey.isHidden() || prey.getEntityLevel() >= hunter.getEntityLevel()) return false;
        }
        return true;
    }

    public static boolean isHumanTargetStillValid(Human hunter, Entity target) {
        if (target == null || !target.isAlive()) return false;
        float dist = hunter.getPosition().distanceTo(target.getPosition());
        if (dist > hunter.getVisionRange() * 1.5f) return false;

        if (target instanceof Animal && !hunter.canHunt((Animal) target)) return false;
        if (target instanceof FoodSource && !hunter.canEatFoodSource((FoodSource) target)) return false;

        return true;
    }
}

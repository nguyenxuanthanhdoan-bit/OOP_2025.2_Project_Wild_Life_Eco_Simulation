package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.items.FoodSource;
import model.living_beings.Animal;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.navigation.TerrainNavigator;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Mushroom;
import model.plants.Plant;
import model.structures.FoodStorage;
import model.structures.Well;
import model.world.World;

import java.util.List;

public class ForageStrategy implements IStrategy {
    private static final float UNSAFE_FOOD_MEMORY_DURATION = 18.0f;
    private static final float FOOD_DANGER_RADIUS = 170.0f;

    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private final PathNavigator waterNavigator = new PathNavigator();
    private final PathNavigator foodNavigator = new PathNavigator();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal) || world == null) return;

        Animal animal = (Animal) owner;
        if (animal instanceof Human) {
            executeHumanForage((Human) animal, world, deltaTime);
        } else {
            executeAnimalForage(animal, world, deltaTime);
        }
    }

    private void executeAnimalForage(Animal animal, World world, float deltaTime) {
        boolean needsWater = animal.getThirst() < animal.getMaxThirst() * Animal.THIRST_WARNING_THRESHOLD;
        boolean needsFood = animal.getHunger() < animal.getMaxHunger() * Animal.HUNGER_WARNING_THRESHOLD;

        double thirstRatio = animal.getThirst() / animal.getMaxThirst();
        double hungerRatio = animal.getHunger() / animal.getMaxHunger();
        if (needsWater && animal.canUseStrategy(HunterStrategy.class)) {
            needsFood = false;
        } else if (needsWater && needsFood) {
            if (hungerRatio < thirstRatio) needsWater = false;
            else needsFood = false;
        }

        double minRatio = Math.min(thirstRatio, hungerRatio);
        float speedMult = (minRatio < 0.1) ? 1.5f : (minRatio < 0.3) ? 1.3f : 1.1f;

        if (needsWater) {
            seekNaturalWater(animal, world, deltaTime, speedMult);
            return;
        }

        if (needsFood) {
            Entity targetFood = findStaticFoodTarget(animal, world);
            if (targetFood != null) {
                moveToAndEatStaticFood(animal, world, targetFood, deltaTime, speedMult);
                return;
            }
        }

        animal.setActionState("idle");
        wanderDelegate.execute(animal, world, deltaTime);
    }

    private void executeHumanForage(Human human, World world, float deltaTime) {
        boolean needsWater = human.getThirst() < human.getMaxThirst() * Animal.THIRST_WARNING_THRESHOLD;
        boolean needsFood = human.getHunger() < human.getMaxHunger() * Animal.HUNGER_WARNING_THRESHOLD;
        boolean criticalHunger = human.getHunger() < human.getMaxHunger() * Animal.CRITICAL_SURVIVAL_THRESHOLD;

        if (human.canHuntRole() && criticalHunger && human.hasCarriedFood()) {
            human.setSpeed(0);
            human.setActionState("eat");
            human.eatCarriedFood(25.0f * deltaTime);
            return;
        }

        if (human.canHuntRole() && needsWater) {
            needsFood = false;
        } else if (needsWater && needsFood) {
            double thirstRatio = human.getThirst() / human.getMaxThirst();
            double hungerRatio = human.getHunger() / human.getMaxHunger();
            if (hungerRatio < thirstRatio) needsWater = false;
            else needsFood = false;
        }

        if (needsWater) {
            Well well = findNearestHomeWell(human, world);
            if (well != null) {
                moveToAndDrinkFromWell(human, world, well, deltaTime);
            } else {
                seekNaturalWater(human, world, deltaTime, 1.0f);
            }
            return;
        }

        if (needsFood) {
            FoodStorage storage = findNearestHomeFoodStorage(human, world, true);
            if (storage != null) {
                moveToAndEatFromStorage(human, world, storage, deltaTime);
                return;
            }

            Entity targetFood = findStaticFoodTarget(human, world);
            if (targetFood != null) {
                moveToAndEatStaticFood(human, world, targetFood, deltaTime, 1.0f);
                return;
            }
        }

        human.setActionState("idle");
        wanderDelegate.execute(human, world, deltaTime);
    }

    private void seekNaturalWater(Animal animal, World world, float deltaTime, float speedMult) {
        if (animal.isNearWater()) {
            animal.setSpeed(0);
            animal.setActionState("drink");
            animal.drink();
            waterNavigator.clear();
            return;
        }

        animal.setActionState(animal instanceof Human ? "walk" : "idle");
        animal.setSpeed(animal.getBaseSpeed() * speedMult);
        Vector2 shorePoint = TerrainNavigator.findNearestShorePoint(animal, world, 1200.0f);
        if (shorePoint != null) {
            waterNavigator.moveTo(animal, world, shorePoint, deltaTime,
                    animal.getSize() / 2 + 20.0f, 1.25f, MovementContext.SEEKING_WATER);
            if (animal.isNearWater()) {
                animal.setSpeed(0);
                animal.setActionState("drink");
                animal.drink();
                waterNavigator.clear();
            }
        }
    }

    private void moveToAndDrinkFromWell(Human human, World world, Well well, float deltaTime) {
        if (well.drink(human)) {
            human.setSpeed(0);
            human.setActionState("drink");
            waterNavigator.clear();
            return;
        }

        human.setActionState("walk");
        human.setSpeed(human.getBaseSpeed());
        Vector2 target = PathNavigator.findInteractionPoint(human, world, well, well.getDrinkRadius());
        waterNavigator.moveTo(human, world, target, deltaTime, 8.0f, 1.2f, MovementContext.SEEKING_STRUCTURE);
    }

    private void moveToAndEatFromStorage(Human human, World world, FoodStorage storage, float deltaTime) {
        float eatRange = human.getSize() / 2 + storage.getSize() / 2 + 18.0f;
        if (human.getPosition().distanceTo(storage.getPosition()) <= eatRange) {
            human.setSpeed(0);
            human.setActionState("eat");
            human.eatFromStorage(storage, 28.0f * deltaTime);
            foodNavigator.clear();
            return;
        }

        human.setActionState("walk");
        human.setSpeed(human.getBaseSpeed());
        Vector2 target = PathNavigator.findInteractionPoint(human, world, storage, eatRange);
        foodNavigator.moveTo(human, world, target, deltaTime, 8.0f, 1.2f, MovementContext.SEEKING_STRUCTURE);
    }

    private void moveToAndEatStaticFood(Animal animal, World world, Entity targetFood,
                                        float deltaTime, float speedMult) {
        float eatRange = animal.getSize() / 2 + targetFood.getSize() / 2 - 2.0f;
        if (animal.getPosition().distanceTo(targetFood.getPosition()) <= eatRange) {
            animal.setSpeed(0);
            animal.setActionState("eat");
            if (targetFood instanceof Plant) {
                animal.eat((Plant) targetFood);
            } else if (targetFood instanceof FoodSource) {
                animal.eatMeat((FoodSource) targetFood);
            }
            return;
        }

        animal.setActionState(animal instanceof Human ? "walk" : "idle");
        animal.setSpeed(animal.getBaseSpeed() * speedMult);
        foodNavigator.moveTo(animal, world, targetFood.getPosition(), deltaTime, eatRange, 1.0f);
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public String getName() {
        return "Forage";
    }

    private int getStaticFoodPriority(Animal animal, Entity entity) {
        if (entity instanceof FoodSource && animal.canEatFoodSource((FoodSource) entity)) return 4;
        if (entity instanceof Fruit && animal.canEatPlant((Fruit) entity)) return 3;
        if (entity instanceof Mushroom && animal.canEatPlant((Mushroom) entity)) return 2;
        if (entity instanceof Grass && animal.canEatPlant((Grass) entity)) return 1;
        if (entity instanceof Plant && animal.canEatPlant((Plant) entity)) return 1;
        return 0;
    }

    private Entity findStaticFoodTarget(Animal animal, World world) {
        if (world.getSpatialGrid() == null) return null;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(animal.getPosition(), (float) animal.getVisionRange());
        Entity targetFood = null;
        float bestScore = Float.MAX_VALUE;

        for (Entity neighbor : neighbors) {
            if (!neighbor.isAlive()) continue;
            if (animal.isInDangerZone(neighbor.getPosition())) continue;
            if (animal.isFoodMarkedUnsafe(neighbor)) continue;

            int priority = getStaticFoodPriority(animal, neighbor);
            if (priority <= 0) continue;
            if (isFoodLocationDangerous(animal, world, neighbor)) {
                animal.markFoodUnsafe(neighbor, UNSAFE_FOOD_MEMORY_DURATION);
                continue;
            }

            float dist = animal.getPosition().distanceTo(neighbor.getPosition());
            float score = dist - (priority * 60.0f);
            if (score < bestScore) {
                bestScore = score;
                targetFood = neighbor;
            }
        }

        return targetFood;
    }

    private Well findNearestHomeWell(Human human, World world) {
        if (human.getHomeSettlement() != null) {
            return human.getHomeSettlement().findNearestWell(human.getPosition());
        }
        Well best = null;
        float bestDist = Float.MAX_VALUE;
        for (Entity entity : getHomeEntities(human, world)) {
            if (!(entity instanceof Well) || !entity.isAlive()) continue;
            float dist = human.getPosition().distanceTo(entity.getPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = (Well) entity;
            }
        }
        return best;
    }

    private FoodStorage findNearestHomeFoodStorage(Human human, World world, boolean requireFood) {
        if (human.getHomeSettlement() != null) {
            return human.getHomeSettlement().findNearestFoodStorage(human.getPosition(), requireFood);
        }
        FoodStorage best = null;
        float bestDist = Float.MAX_VALUE;
        for (Entity entity : getHomeEntities(human, world)) {
            if (!(entity instanceof FoodStorage) || !entity.isAlive()) continue;
            FoodStorage storage = (FoodStorage) entity;
            if (requireFood && !storage.hasFood()) continue;
            float dist = human.getPosition().distanceTo(storage.getPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = storage;
            }
        }
        return best;
    }

    private List<Entity> getHomeEntities(Human human, World world) {
        if (world.getSpatialGrid() != null) {
            return world.getSpatialGrid().getNeighbors(human.getHomeCenter(), human.getHomeRadius());
        }
        return world.getEntities();
    }

    private boolean isFoodLocationDangerous(Animal ownerAnimal, World world, Entity food) {
        if (!ownerAnimal.getProfile().canBeScared() || world.getSpatialGrid() == null) {
            return false;
        }

        List<Entity> nearby = world.getSpatialGrid().getNeighbors(food.getPosition(), FOOD_DANGER_RADIUS);
        for (Entity entity : nearby) {
            if (!(entity instanceof Animal) || entity == ownerAnimal || !entity.isAlive()) continue;

            Animal other = (Animal) entity;
            if (ownerAnimal.isThreatenedBy(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public core.Vector2 getTarget() {
        if (waterNavigator.getLastTarget() != null) return waterNavigator.getLastTarget();
        if (foodNavigator.getLastTarget() != null) return foodNavigator.getLastTarget();
        return wanderDelegate.getTarget();
    }

    @Override
    public java.util.List<core.Vector2> getPath() {
        if (waterNavigator.getLastTarget() != null) return waterNavigator.getPath();
        if (foodNavigator.getLastTarget() != null) return foodNavigator.getPath();
        return wanderDelegate.getPath();
    }
}

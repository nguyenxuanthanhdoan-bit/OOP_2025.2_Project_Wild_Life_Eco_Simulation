package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.navigation.TerrainNavigator;
import model.world.World;
import model.plants.Grass;
import model.plants.Fruit;
import model.plants.Mushroom;
import model.items.Carcass;
import model.items.FoodSource;
import java.util.List;

public class ForageStrategy implements IStrategy {
    private static final float UNSAFE_FOOD_MEMORY_DURATION = 18.0f;
    private static final float FOOD_DANGER_RADIUS = 170.0f;

    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private final PathNavigator waterNavigator = new PathNavigator();
    private final PathNavigator foodNavigator = new PathNavigator();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        if (world == null) return;

        boolean needsWater = ownerAnimal.getThirst() < ownerAnimal.getMaxThirst() * Animal.THIRST_WARNING_THRESHOLD;
        boolean needsFood = ownerAnimal.getHunger() < ownerAnimal.getMaxHunger() * Animal.HUNGER_WARNING_THRESHOLD;

        double thirstRatio = ownerAnimal.getThirst() / ownerAnimal.getMaxThirst();
        double hungerRatio = ownerAnimal.getHunger() / ownerAnimal.getMaxHunger();

        if (needsWater && needsFood) {
            if (hungerRatio < thirstRatio) {
                needsWater = false; // Đói hơn khát -> Đi ăn trước
            } else {
                needsFood = false; // Khát hơn đói -> Đi uống trước
            }
        }

        // Tốc độ động: càng đói/khát thì đi càng nhanh
        double minRatio = Math.min(
            ownerAnimal.getThirst() / ownerAnimal.getMaxThirst(),
            ownerAnimal.getHunger() / ownerAnimal.getMaxHunger()
        );
        float speedMult = (minRatio < 0.1) ? 1.5f : (minRatio < 0.3) ? 1.3f : 1.1f;

        // Ưu tiên 1: Nước (nếu khát)
        if (needsWater) {
            if (ownerAnimal.isNearWater()) {
                ownerAnimal.setSpeed(0);
                ownerAnimal.setActionState("drink");
                ownerAnimal.drink();
                waterNavigator.clear();
                return;
            } else {
                ownerAnimal.setActionState("idle");
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * speedMult);

                Vector2 shorePoint = TerrainNavigator.findNearestShorePoint(ownerAnimal, world, 1200.0f);
                if (shorePoint != null) {
                    waterNavigator.moveTo(ownerAnimal, world, shorePoint, deltaTime,
                            ownerAnimal.getSize() / 2 + 20.0f, 1.25f, MovementContext.SEEKING_WATER);
                    if (ownerAnimal.isNearWater()) {
                        ownerAnimal.setSpeed(0);
                        ownerAnimal.setActionState("drink");
                        ownerAnimal.drink();
                        waterNavigator.clear();
                    }
                }
                return;
            }
        }

        // Ưu tiên 2: Thức ăn (nếu đói)
        if (needsFood) {
            if (world.getSpatialGrid() == null) return;

            List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());
            Entity targetFood = null;
            float bestScore = Float.MAX_VALUE;

            for (Entity neighbor : neighbors) {
                if (!neighbor.isAlive()) continue;
                if (ownerAnimal.isFoodMarkedUnsafe(neighbor)) continue;

                int priority = 0;
                if (ownerAnimal.getDietType() == DietType.HERBIVORE) {
                    if (neighbor instanceof Fruit && ownerAnimal.canEatPlant((Fruit) neighbor)) priority = 3;
                    else if (neighbor instanceof Mushroom && ownerAnimal.canEatPlant((Mushroom) neighbor)) priority = 2;
                    else if (neighbor instanceof Grass && ownerAnimal.canEatPlant((Grass) neighbor)) priority = 1;
                } else if (ownerAnimal.getDietType() == DietType.CARNIVORE) {
                    // Thú ăn thịt CHỈ ăn thịt — ForageStrategy không tìm con mồi sống
                    // (việc đó là của HunterStrategy)
                    if (neighbor instanceof FoodSource && canEatMeatSource(ownerAnimal, (FoodSource) neighbor)) priority = 1;
                } else if (ownerAnimal.getDietType() == DietType.OMNIVORE) {
                    if (neighbor instanceof FoodSource && canEatMeatSource(ownerAnimal, (FoodSource) neighbor)) priority = 4;
                    else if (neighbor instanceof Fruit && ownerAnimal.canEatPlant((Fruit) neighbor)) priority = 3;
                    else if (neighbor instanceof Mushroom && ownerAnimal.canEatPlant((Mushroom) neighbor)) priority = 2;
                    else if (neighbor instanceof Grass && ownerAnimal.canEatPlant((Grass) neighbor)) priority = 1;
                }

                if (priority > 0) {
                    if (isFoodLocationDangerous(ownerAnimal, world, neighbor)) {
                        ownerAnimal.markFoodUnsafe(neighbor, UNSAFE_FOOD_MEMORY_DURATION);
                        continue;
                    }

                    float dist = ownerAnimal.getPosition().distanceTo(neighbor.getPosition());
                    float score = dist - (priority * 60.0f);
                    if (score < bestScore) {
                        bestScore = score;
                        targetFood = neighbor;
                    }
                }
            }

            if (targetFood != null) {
                float eatRange = ownerAnimal.getSize() / 2 + targetFood.getSize() / 2 - 2.0f;
                if (ownerAnimal.getPosition().distanceTo(targetFood.getPosition()) <= eatRange) {
                    ownerAnimal.setSpeed(0);
                    ownerAnimal.setActionState("eat");
                    if (targetFood instanceof model.plants.Plant) {
                        ownerAnimal.eat((model.plants.Plant) targetFood);
                    } else if (targetFood instanceof FoodSource) {
                        ownerAnimal.eatMeat((model.items.FoodSource) targetFood);
                    }
                } else {
                    ownerAnimal.setActionState("idle");
                    ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * speedMult);
                    foodNavigator.moveTo(ownerAnimal, world, targetFood.getPosition(), deltaTime, eatRange, 1.0f);
                }
                return;
            }
        }

        // Nếu no/đủ nước, đi dạo bình thường
        if (ownerAnimal.getSpeed() != ownerAnimal.getBaseSpeed()) {
            ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
        }
        ownerAnimal.setActionState("idle");
        wanderDelegate.execute(owner, world, deltaTime);
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

    private boolean canEatMeatSource(Animal animal, FoodSource food) {
        if (food instanceof Carcass) {
            Carcass carcass = (Carcass) food;
            return !carcass.getSourceSpecies().equals(animal.getSpeciesName());
        }
        return true;
    }

    private boolean isFoodLocationDangerous(Animal ownerAnimal, World world, Entity food) {
        if (ownerAnimal.getDietType() != DietType.HERBIVORE || world.getSpatialGrid() == null) {
            return false;
        }

        List<Entity> nearby = world.getSpatialGrid().getNeighbors(food.getPosition(), FOOD_DANGER_RADIUS);
        for (Entity entity : nearby) {
            if (!(entity instanceof Animal) || entity == ownerAnimal || !entity.isAlive()) continue;

            Animal other = (Animal) entity;
            if (other.getDietType() == DietType.CARNIVORE
                    && other.getEntityLevel() > ownerAnimal.getEntityLevel()
                    && other.isAliveState()) {
                return true;
            }
        }
        return false;
    }
}

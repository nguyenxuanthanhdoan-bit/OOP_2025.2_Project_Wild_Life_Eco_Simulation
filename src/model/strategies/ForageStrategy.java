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
import model.plants.Plant;
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

        if (needsWater && ownerAnimal.canUseStrategy(HunterStrategy.class)) {
            needsFood = false; // Carnivore đang khát thì uống trước; cơn đói do HunterStrategy xử lý sau.
        } else if (needsWater && needsFood) {
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

                int priority = getStaticFoodPriority(ownerAnimal, neighbor);

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
                    if (targetFood instanceof Plant) {
                        ownerAnimal.eat((Plant) targetFood);
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

    private int getStaticFoodPriority(Animal animal, Entity entity) {
        if (entity instanceof FoodSource && animal.canEatFoodSource((FoodSource) entity)) return 4;
        if (entity instanceof Fruit && animal.canEatPlant((Fruit) entity)) return 3;
        if (entity instanceof Mushroom && animal.canEatPlant((Mushroom) entity)) return 2;
        if (entity instanceof Grass && animal.canEatPlant((Grass) entity)) return 1;
        if (entity instanceof Plant && animal.canEatPlant((Plant) entity)) return 1;
        return 0;
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

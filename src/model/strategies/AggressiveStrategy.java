package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.world.World;
import model.entity.Entity;
import java.util.Random;

public class AggressiveStrategy implements IStrategy {
    private Vector2 wanderDirection = new Vector2();
    private float stateTimer;
    private Random random = new Random();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;
        
        // Hoảng loạn chạy x2 tốc độ tìm đường sống
        ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * 2.0f);

        stateTimer -= deltaTime;
        if (stateTimer <= 0) {
            float dx = (random.nextFloat() * 2) - 1;
            float dy = (random.nextFloat() * 2) - 1;
            wanderDirection.set(dx, dy);
            if (wanderDirection.lengthSquared() > 0) {
                wanderDirection.normalize();
            } else {
                wanderDirection.set(1, 0);
            }
            stateTimer = 0.3f + random.nextFloat() * 0.7f; // Đổi hướng rất nhanh liên tục

            if (wanderDirection.x > 0) ownerAnimal.setFacingRight(true);
            else if (wanderDirection.x < 0) ownerAnimal.setFacingRight(false);
        }

        // Khi đang hoảng loạn, nếu vô tình đạp trúng nước thì lập tức uống
        if (ownerAnimal.isNearWater()) {
            ownerAnimal.setSpeed(0);
            ownerAnimal.drink();
            return;
        }

        // Nếu vô tình giẫm phải thức ăn thì ăn luôn
        if (world.getSpatialGrid() != null) {
            java.util.List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), ownerAnimal.getSize() * 2);
            for (Entity neighbor : neighbors) {
                boolean isFood = false;
                if (ownerAnimal.getDietType() == model.living_beings.DietType.HERBIVORE) {
                    if (neighbor instanceof model.plants.Grass || neighbor instanceof model.plants.Fruit || neighbor instanceof model.plants.Mushroom) isFood = true;
                } else if (ownerAnimal.getDietType() == model.living_beings.DietType.CARNIVORE) {
                    if (neighbor instanceof model.items.Meat || neighbor instanceof model.plants.Fruit || neighbor instanceof model.plants.Mushroom) isFood = true;
                } else if (ownerAnimal.getDietType() == model.living_beings.DietType.OMNIVORE) {
                    if (neighbor instanceof model.plants.Grass || neighbor instanceof model.plants.Fruit || neighbor instanceof model.items.Meat || neighbor instanceof model.plants.Mushroom) isFood = true;
                }

                if (isFood && neighbor.isAlive()) {
                    float eatRange = ownerAnimal.getSize() / 2 + neighbor.getSize() / 2 + 10.0f;
                    if (ownerAnimal.getPosition().distanceTo(neighbor.getPosition()) <= eatRange) {
                        ownerAnimal.setSpeed(0);
                        if (neighbor instanceof model.plants.Plant) {
                            ownerAnimal.eat((model.plants.Plant) neighbor);
                        } else if (neighbor instanceof model.items.Meat) {
                            ownerAnimal.eatMeat((model.items.Meat) neighbor);
                        }
                        return; // Đang ăn, ngừng chạy
                    }
                }
            }
        }

        ownerAnimal.move(wanderDirection, deltaTime);
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public String getName() {
        return "Aggressive";
    }
}

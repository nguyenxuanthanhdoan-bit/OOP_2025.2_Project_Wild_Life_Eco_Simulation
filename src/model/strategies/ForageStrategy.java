package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.living_beings.LivingBeing;
import model.world.World;
import model.plants.Grass;
import model.plants.Fruit;
import model.plants.Mushroom;
import model.items.Meat;
import java.util.List;

public class ForageStrategy implements IStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();

    /**
     * [MỚI] Obstacle Avoidance Steering.
     * Phát hiện vật cản cứng (cây) trong phạm vi gần và tạo lực đẩy ra khỏi chúng.
     * Kết hợp lực đẩy này với hướng di chuyển mong muốn để lách qua cây.
     */
    private Vector2 steer(Vector2 desiredDir, Animal animal, World world) {
        if (world.getSpatialGrid() == null) return desiredDir;

        Vector2 avoidForce = new Vector2(0, 0);
        int obstacleCount = 0;

        // Quét vật cản trong phạm vi = kích thước bản thân + kích thước cây + buffer
        float scanRadius = animal.getSize() / 2 + 150.0f;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(animal.getPosition(), scanRadius);

        for (Entity e : nearby) {
            if (!e.isSolid() || !e.isAlive() || e == animal) continue;

            Vector2 toObstacle = e.getPosition().copy().subtract(animal.getPosition());
            float dist = toObstacle.length();
            if (dist < 0.01f) continue;

            // Ngưỡng tạo lực: tổng 2 bán kính + 80px buffer
            float threshold = animal.getSize() / 2 + e.getSize() / 2 + 80.0f;

            if (dist < threshold) {
                // Lực đẩy: mạnh dần khi tiến gần (quadratic)
                float t = 1.0f - (dist / threshold);
                float strength = t * t * 3.0f;
                // Hướng đẩy = ngược chiều từ vật cản về con vật
                avoidForce.x -= (toObstacle.x / dist) * strength;
                avoidForce.y -= (toObstacle.y / dist) * strength;
                obstacleCount++;
            }
        }

        if (obstacleCount == 0) return desiredDir;

        // Kết hợp hướng mong muốn + lực né vật cản
        Vector2 result = new Vector2(
            desiredDir.x + avoidForce.x,
            desiredDir.y + avoidForce.y
        );
        if (result.lengthSquared() > 0.0001f) {
            result.normalize();
        } else {
            // Nếu lực đẩy triệt tiêu hoàn toàn hướng mong muốn, đi vuông góc
            result = new Vector2(-desiredDir.y, desiredDir.x);
        }
        return result;
    }

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

        // Ưu tiên 1: Nước (nếu khát)
        if (needsWater) {
            if (ownerAnimal.isNearWater()) {
                ownerAnimal.setSpeed(0);
                ownerAnimal.drink();
                return;
            } else {
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * 1.5f);

                // Tìm ô nước gần nhất trong phạm vi lớn
                Vector2 bestWaterPos = null;
                float closestDistSq = Float.MAX_VALUE;

                float searchRadius = 1200.0f;
                int steps = 24; // Nhiều góc quét hơn để tìm hồ chính xác hơn

                for (int i = 0; i < steps; i++) {
                    float angle = (float) (i * 2 * Math.PI / steps);
                    for (float r = 50.0f; r <= searchRadius; r += 50.0f) {
                        float cx = ownerAnimal.getPosition().x + (float)Math.cos(angle) * r;
                        float cy = ownerAnimal.getPosition().y + (float)Math.sin(angle) * r;

                        if (world.isPositionInWater(cx, cy)) {
                            float distSq = ownerAnimal.getPosition().distanceSquared(new Vector2(cx, cy));
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                bestWaterPos = new Vector2(cx, cy);
                            }
                            break;
                        }
                    }
                }

                Vector2 desiredDir;
                if (bestWaterPos != null) {
                    desiredDir = bestWaterPos.subtract(ownerAnimal.getPosition());
                } else {
                    // Dự phòng: đi về tâm bản đồ
                    Vector2 center = new Vector2(world.getWidth() / 2.0f, world.getHeight() / 2.0f);
                    desiredDir = center.subtract(ownerAnimal.getPosition());
                }

                if (desiredDir.lengthSquared() > 0) {
                    desiredDir.normalize();
                    // [QUAN TRỌNG] Áp dụng lực né cây trước khi di chuyển
                    Vector2 finalDir = steer(desiredDir, ownerAnimal, world);
                    if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
                    else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
                    ownerAnimal.move(finalDir, deltaTime);
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

                int priority = 0;
                if (ownerAnimal.getDietType() == DietType.HERBIVORE) {
                    if (neighbor instanceof Fruit) priority = 3;
                    else if (neighbor instanceof Mushroom) priority = 2;
                    else if (neighbor instanceof Grass) priority = 1;
                } else if (ownerAnimal.getDietType() == DietType.CARNIVORE) {
                    if (neighbor instanceof Meat) priority = 3;
                    else if (neighbor instanceof Fruit) priority = 2;
                    else if (neighbor instanceof Mushroom) priority = 1;
                } else if (ownerAnimal.getDietType() == DietType.OMNIVORE) {
                    if (neighbor instanceof Meat) priority = 4;
                    else if (neighbor instanceof Fruit) priority = 3;
                    else if (neighbor instanceof Mushroom) priority = 2;
                    else if (neighbor instanceof Grass) priority = 1;
                }

                if (priority > 0) {
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
                    if (targetFood instanceof model.plants.Plant) {
                        ownerAnimal.eat((model.plants.Plant) targetFood);
                    } else if (targetFood instanceof Meat) {
                        ownerAnimal.eatMeat((Meat) targetFood);
                    }
                } else {
                    ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * 1.5f);
                    Vector2 desiredDir = targetFood.getPosition().copy().subtract(ownerAnimal.getPosition());
                    if (desiredDir.lengthSquared() > 0) {
                        desiredDir.normalize();
                        // [QUAN TRỌNG] Áp dụng lực né cây trước khi di chuyển
                        Vector2 finalDir = steer(desiredDir, ownerAnimal, world);
                        if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
                        else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
                        ownerAnimal.move(finalDir, deltaTime);
                    }
                }
                return;
            }
        }

        // Nếu no/đủ nước, đi dạo bình thường
        if (ownerAnimal.getSpeed() != ownerAnimal.getBaseSpeed()) {
            ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
        }
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
}

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

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        if (world == null || world.getSpatialGrid() == null) return;

        // Quét các thực thể xung quanh trong tầm nhìn
        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());

        List<Animal> predators = new ArrayList<>();
        Bush closestBush = null;
        float closestBushDist = Float.MAX_VALUE;

        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                Animal other = (Animal) neighbor;
                if (other.isAliveState() && other.getDietType() == DietType.CARNIVORE) {
                    predators.add(other);
                }
            } else if (neighbor instanceof Bush) {
                Bush bush = (Bush) neighbor;
                // Bụi cây hợp lệ nếu chưa có ai nằm hoặc chính mình đang nằm trong đó
                if (!bush.isOccupied() || (ownerAnimal.getPosition().distanceTo(bush.getPosition()) <= bush.getRadius())) {
                    float dist = ownerAnimal.getPosition().distanceTo(bush.getPosition());
                    if (dist < closestBushDist) {
                        closestBushDist = dist;
                        closestBush = bush;
                    }
                }
            }
        }

        // Nếu phát hiện kẻ săn mồi
        if (!predators.isEmpty()) {
            if (closestBush != null) {
                float distToBush = ownerAnimal.getPosition().distanceTo(closestBush.getPosition());
                if (distToBush <= closestBush.getRadius()) {
                    // Đã vào bụi cây -> ẩn nấp
                    if (!ownerAnimal.isHidden()) {
                        ownerAnimal.hideInBush(closestBush);
                    }
                } else {
                    // Đang chạy đến bụi cây
                    if (ownerAnimal.isHidden()) {
                        ownerAnimal.exitBush();
                    }
                    ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.5f));
                    Vector2 dirToBush = closestBush.getPosition().copy().subtract(ownerAnimal.getPosition());
                    if (dirToBush.lengthSquared() > 0) {
                        dirToBush.normalize();
                    }

                    if (dirToBush.x > 0) {
                        ownerAnimal.setFacingRight(true);
                    } else if (dirToBush.x < 0) {
                        ownerAnimal.setFacingRight(false);
                    }
                    ownerAnimal.move(dirToBush, deltaTime);
                }
            } else {
                // Không tìm thấy bụi cây -> chạy trốn ngược hướng kẻ thù
                if (ownerAnimal.isHidden()) {
                    ownerAnimal.exitBush();
                }
                ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.5f));

                Vector2 fleeDir = new Vector2();
                for (Animal predator : predators) {
                    Vector2 diff = ownerAnimal.getPosition().copy().subtract(predator.getPosition());
                    if (diff.lengthSquared() > 0) {
                        diff.normalize();
                        fleeDir.add(diff);
                    }
                }
                if (fleeDir.lengthSquared() > 0) {
                    fleeDir.normalize();
                } else {
                    fleeDir.set(1, 0);
                }

                if (fleeDir.x > 0) {
                    ownerAnimal.setFacingRight(true);
                } else if (fleeDir.x < 0) {
                    ownerAnimal.setFacingRight(false);
                }
                ownerAnimal.move(fleeDir, deltaTime);
            }
        } else {
            // Không có mối đe dọa -> ra khỏi bụi (nếu có) và đi dạo
            if (ownerAnimal.isHidden()) {
                ownerAnimal.exitBush();
            }
            if (ownerAnimal.getSpeed() != ownerAnimal.getBaseSpeed()) {
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            }
            wanderDelegate.execute(owner, world, deltaTime);
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

package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.world.World;
import model.entity.Entity;
import java.util.List;

public class HunterStrategy implements IStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private Animal targetPrey = null;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        // Kiểm tra xem mục tiêu cũ còn hợp lệ không (còn sống, không trốn, trong tầm nhìn)
        if (targetPrey != null) {
            float dist = ownerAnimal.getPosition().distanceTo(targetPrey.getPosition());
            if (!targetPrey.isAliveState() || targetPrey.isHidden() || dist > ownerAnimal.getVisionRange()) {
                targetPrey = null;
                ownerAnimal.setActionState("idle");
            }
        }

        // Quét tìm mục tiêu mới nếu chưa có
        if (targetPrey == null && world != null && world.getSpatialGrid() != null) {
            List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());
            float closestDist = Float.MAX_VALUE;
            for (Entity neighbor : neighbors) {
                if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                    Animal other = (Animal) neighbor;
                    // Săn động vật ăn cỏ có kích thước nhỏ hơn bản thân kẻ săn mồi
                    if (other.isAliveState() && other.getDietType() == DietType.HERBIVORE && !other.isHidden() && other.getSize() < ownerAnimal.getSize()) {
                        float dist = ownerAnimal.getPosition().distanceTo(other.getPosition());
                        if (dist < closestDist) {
                            closestDist = dist;
                            targetPrey = other;
                        }
                    }
                }
            }
        }

        // Nếu có mục tiêu -> đuổi bắt và tấn công
        if (targetPrey != null) {
            Vector2 dirToPrey = targetPrey.getPosition().copy().subtract(ownerAnimal.getPosition());
            float distToPrey = dirToPrey.length();
            if (distToPrey > 0) {
                dirToPrey.normalize();
            }

            if (dirToPrey.x > 0) {
                ownerAnimal.setFacingRight(true);
            } else if (dirToPrey.x < 0) {
                ownerAnimal.setFacingRight(false);
            }

            // Tấn công khi đủ gần
            float attackRange = ownerAnimal.getSize() / 2 + targetPrey.getSize() / 2 + 10.0f;
            if (distToPrey <= attackRange) {
                // Đứng yên tại chỗ, không di chuyển tiếp để tránh giật lắc
                ownerAnimal.setSpeed(0);
                ownerAnimal.setActionState("attack");

                float damage = (ownerAnimal instanceof model.living_beings.Tiger) ? 40.0f : 20.0f;
                targetPrey.takeDamage(damage * deltaTime);

                // Nếu mục tiêu chết -> hồi đói và reset trạng thái hành động
                if (!targetPrey.isAliveState()) {
                    ownerAnimal.setHunger(Math.min(ownerAnimal.getMaxHunger(), ownerAnimal.getHunger() + 50.0));
                    targetPrey = null;
                    ownerAnimal.setActionState("idle");
                }
            } else {
                // Đang đuổi bắt mục tiêu ở tốc độ cao
                ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.5f));
                ownerAnimal.setActionState("idle");
                ownerAnimal.move(dirToPrey, deltaTime);
            }
        } else {
            // Không có con mồi -> đi lang thang bình thường
            ownerAnimal.setActionState("idle");
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
        return 3;
    }

    @Override
    public String getName() {
        return "Hunter";
    }
}

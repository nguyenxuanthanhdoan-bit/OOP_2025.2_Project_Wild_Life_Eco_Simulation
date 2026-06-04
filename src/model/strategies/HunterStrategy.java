package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.items.Carcass;
import model.world.World;
import model.entity.Entity;
import java.util.List;

public class HunterStrategy implements IStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private Entity targetFood = null;
    private float reassessTimer = 0f;
    private static final double RUN_COST_MULTIPLIER = 3.0;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;

        // Bỏ qua nếu đã no
        if (ownerAnimal.getHunger() >= ownerAnimal.getMaxHunger() * 0.95) {
            targetFood = null;
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Kiểm tra xem mục tiêu cũ còn hợp lệ không
        if (targetFood != null) {
            float dist = ownerAnimal.getPosition().distanceTo(targetFood.getPosition());
            if (!targetFood.isAlive() || dist > ownerAnimal.getVisionRange()) {
                targetFood = null;
                ownerAnimal.setActionState("idle");
            } else if (targetFood instanceof Animal) {
                Animal prey = (Animal) targetFood;
                if (prey.isHidden()) {
                    targetFood = null;
                    ownerAnimal.setActionState("idle");
                } else if (reassessTimer > 1.0f) {
                    // Định kỳ 1 giây đánh giá lại mục tiêu xem có con mồi nào ngon hơn/gần hơn không
                    targetFood = null;
                    reassessTimer = 0f;
                }
            }
        }

        reassessTimer += deltaTime;

        // Quét tìm mục tiêu mới
        if (targetFood == null && world != null && world.getSpatialGrid() != null) {
            List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());
            
            float bestScore = -1.0f;
            Entity bestTarget = null;
            
            boolean isVeryHungry = ownerAnimal.getHunger() < ownerAnimal.getMaxHunger() * 0.6;

            for (Entity neighbor : neighbors) {
                if (!neighbor.isAlive()) continue;

                float dist = ownerAnimal.getPosition().distanceTo(neighbor.getPosition());
                float score = 0;

                if (neighbor instanceof Carcass) {
                    Carcass carcass = (Carcass) neighbor;
                    // Tránh ăn thịt đồng loại
                    if (carcass.getSourceSpecies().equals(ownerAnimal.getSpeciesName())) {
                        continue;
                    }
                    
                    // Điểm cơ bản cho Carcass
                    score = 1000.0f - dist;
                    if (isVeryHungry) {
                        score += 5000.0f; // Rất đói thì ưu tiên xác thối nhất
                    }
                    
                } else if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                    Animal other = (Animal) neighbor;
                    
                    // Không săn đồng loại hoặc con mồi quá to, hoặc đang trốn
                    if (other.isHidden() || other.getSize() > ownerAnimal.getSize() * 1.5f 
                            || other.getSpeciesName().equals(ownerAnimal.getSpeciesName())) {
                        continue;
                    }

                    // Hạn chế săn thú ăn thịt khác trừ khi cực kỳ đói
                    if (other.getDietType() == model.living_beings.DietType.CARNIVORE && !isVeryHungry) {
                        continue;
                    }

                    score = 500.0f - dist;
                    if (other.getDietType() == model.living_beings.DietType.CARNIVORE) {
                        score -= 300.0f; // Trừ điểm để ưu tiên săn thú ăn cỏ hơn nếu có cả hai
                    }
                    
                    // Ưu tiên mồi yếu (yếu máu hoặc già)
                    boolean isWeak = other.getHealth() < other.getMaxHealth() * 0.5 || other.getAge() > other.getMaxAge() * 0.8;
                    if (isWeak) {
                        score += 2000.0f;
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = neighbor;
                }
            }
            
            targetFood = bestTarget;
        }

        // Nếu có mục tiêu
        if (targetFood != null) {
            Vector2 dirToFood = targetFood.getPosition().copy().subtract(ownerAnimal.getPosition());
            float distToFood = dirToFood.length();
            if (distToFood > 0) {
                dirToFood.normalize();
            }

            if (dirToFood.x > 0) {
                ownerAnimal.setFacingRight(true);
            } else if (dirToFood.x < 0) {
                ownerAnimal.setFacingRight(false);
            }

            float interactRange;
            if (targetFood instanceof Carcass) {
                // Ăn xác: giữ nguyên tầm ăn nhỏ, kết hợp với lực đẩy sẽ tạo ra hiệu ứng tranh giành mồi (chen lấn)
                interactRange = ownerAnimal.getSize() / 2 + targetFood.getSize() / 2 - 8.0f;
            } else {
                // Tấn công con mồi sống: áp sát là đủ, +5px để trigger chắc hơn
                interactRange = ownerAnimal.getSize() / 2 + targetFood.getSize() / 2 + 5.0f;
            }
            if (distToFood <= interactRange) {
                ownerAnimal.setSpeed(0);
                
                if (targetFood instanceof Carcass) {
                    // Đứng ăn xác
                    ownerAnimal.setActionState("eat"); // Hoặc idle nếu không có animation
                    ownerAnimal.eatCarcass((Carcass) targetFood, deltaTime);
                    
                    // Nếu ăn no hoặc xác biến mất
                    if (!targetFood.isAlive() || ownerAnimal.getHunger() >= ownerAnimal.getMaxHunger()) {
                        targetFood = null;
                        ownerAnimal.setActionState("idle");
                    }
                } else if (targetFood instanceof Animal) {
                    // Tấn công con mồi
                    ownerAnimal.setActionState("attack");
                    Animal prey = (Animal) targetFood;
                    // Tăng sát thương để bắt mồi nhanh hơn (Sói 80/s, Hổ 100/s)
                    float damage = (ownerAnimal.getSpeciesName().equals("Hổ")) ? 100.0f : 80.0f;
                    prey.takeDamage(damage * deltaTime);

                    if (!prey.isAlive()) {
                        // Trả về null để lần sau quét trúng Carcass vừa rơi ra
                        targetFood = null;
                        ownerAnimal.setActionState("idle");
                    }
                }
            } else {
                // Đang di chuyển tới thức ăn
                ownerAnimal.setActionState("run");
                
                if (targetFood instanceof Animal) {
                    // Đuổi con mồi -> chạy nhanh, tốn năng lượng
                    double extraHunger = ownerAnimal.getHungerDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
                    double extraThirst = ownerAnimal.getThirstDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
                    ownerAnimal.setHunger(ownerAnimal.getHunger() - extraHunger);
                    ownerAnimal.setThirst(ownerAnimal.getThirst() - extraThirst);
                    ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.5f));
                } else {
                    // Đi tới xác chết -> đi bộ bình thường để tiết kiệm sức
                    ownerAnimal.setSpeed((float) (ownerAnimal.getBaseSpeed() * 1.0f));
                }
                
                // Sử dụng thuật toán Context Steering thông minh để lách vật cản trong lúc đi săn
                Vector2 desiredDir = targetFood.getPosition().copy().subtract(ownerAnimal.getPosition());
                if (desiredDir.lengthSquared() > 0) desiredDir.normalize();
                
                Vector2 bestDir = steer(desiredDir, ownerAnimal, world);
                
                // Cơ chế gỡ kẹt (Escape Mode) nếu bị dồn vào góc chữ U
                float dotProduct = bestDir.x * desiredDir.x + bestDir.y * desiredDir.y;
                if (bestDir.lengthSquared() == 0 || dotProduct < -0.3f) {
                    ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * 0.5f);
                    if (bestDir.lengthSquared() == 0) {
                        bestDir = new Vector2(0, -1);
                    }
                }

                if (bestDir.x > 0) ownerAnimal.setFacingRight(true);
                else if (bestDir.x < 0) ownerAnimal.setFacingRight(false);
                
                ownerAnimal.move(bestDir, deltaTime);
            }
        } else {
            // Không có con mồi/thức ăn -> đi lang thang
            ownerAnimal.setActionState("idle");
            if (ownerAnimal.getSpeed() != ownerAnimal.getBaseSpeed()) {
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            }
            wanderDelegate.execute(owner, world, deltaTime);
        }
    }

    /**
     * Context Steering logic for Hunters to avoid obstacles while chasing.
     */
    private Vector2 steer(Vector2 desiredDir, Animal animal, World world) {
        if (world.getSpatialGrid() == null) return desiredDir;

        Vector2 avoidForce = new Vector2(0, 0);
        int obstacleCount = 0;

        float scanRadius = animal.getSize() / 2 + 150.0f;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(animal.getPosition(), scanRadius);

        for (Entity e : nearby) {
            if (!e.isAlive() || e == animal) continue;

            Vector2 toObstacle = e.getPosition().copy().subtract(animal.getPosition());
            float dist = toObstacle.length();
            if (dist < 0.01f) continue;

            if (e.isSolid()) {
                float threshold = animal.getSize() / 2 + e.getSize() / 2 + 80.0f;
                if (dist < threshold) {
                    float t = 1.0f - (dist / threshold);
                    float strength = t * t * 3.0f;
                    avoidForce.x -= (toObstacle.x / dist) * strength;
                    avoidForce.y -= (toObstacle.y / dist) * strength;
                    obstacleCount++;
                }
            } else if (e instanceof Animal) {
                // Thêm lực đẩy nhẹ (personal space) để các con sói không đè lên nhau thành 1 cục
                float threshold = animal.getSize() / 2 + e.getSize() / 2;
                if (dist < threshold) {
                    float t = 1.0f - (dist / threshold);
                    float strength = t * 1.5f;
                    avoidForce.x -= (toObstacle.x / dist) * strength;
                    avoidForce.y -= (toObstacle.y / dist) * strength;
                    obstacleCount++;
                }
            }
        }

        if (obstacleCount == 0) return desiredDir;

        Vector2 result = new Vector2(
            desiredDir.x + avoidForce.x,
            desiredDir.y + avoidForce.y
        );
        if (result.lengthSquared() > 0.0001f) {
            result.normalize();
        } else {
            result = new Vector2(-desiredDir.y, desiredDir.x);
        }
        return result;
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

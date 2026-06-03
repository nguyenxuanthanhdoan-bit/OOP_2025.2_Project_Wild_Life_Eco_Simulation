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
                }
            }
        }

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
                    
                    // Săn nếu đối phương không nấp và kích thước phù hợp
                    if (!other.isHidden() && other.getSize() <= ownerAnimal.getSize()) {
                        score = 500.0f - dist;
                        
                        // Ưu tiên mồi yếu (yếu máu hoặc già)
                        boolean isWeak = other.getHealth() < other.getMaxHealth() * 0.5 || other.getAge() > other.getMaxAge() * 0.8;
                        if (isWeak) {
                            score += 2000.0f;
                        }
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

            // Xử lý ăn/tấn công khi đủ gần
            float interactRange = ownerAnimal.getSize() / 2 + targetFood.getSize() / 2 + 10.0f;
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
                    float damage = (ownerAnimal.getSpeciesName().equals("Hổ")) ? 40.0f : 20.0f;
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
                
                Vector2 finalDir = dirToFood.copy();
                Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(ownerAnimal, world);
                if (avoidance.lengthSquared() > 0) {
                    finalDir.add(avoidance);
                    if (finalDir.lengthSquared() > 0) finalDir.normalize();
                }

                if (finalDir.x > 0) ownerAnimal.setFacingRight(true);
                else if (finalDir.x < 0) ownerAnimal.setFacingRight(false);
                
                ownerAnimal.move(finalDir, deltaTime);
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

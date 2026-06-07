package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.items.Carcass;
import model.items.FoodSource;
import model.navigation.PathNavigator;
import model.world.World;
import model.entity.Entity;
import java.util.List;

public class HunterStrategy implements IStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private final PathNavigator targetNavigator = new PathNavigator();
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
            targetNavigator.clear();
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        // Kiểm tra xem mục tiêu cũ còn hợp lệ không
        if (targetFood != null) {
            float dist = ownerAnimal.getPosition().distanceTo(targetFood.getPosition());
            if (!targetFood.isAlive() || dist > ownerAnimal.getVisionRange()) {
                targetFood = null;
                targetNavigator.clear();
                ownerAnimal.setActionState("idle");
            } else if (targetFood instanceof Animal) {
                Animal prey = (Animal) targetFood;
                if (prey.isHidden() || prey.getEntityLevel() >= ownerAnimal.getEntityLevel()) {
                    targetFood = null;
                    targetNavigator.clear();
                    ownerAnimal.setActionState("idle");
                } else if (reassessTimer > 1.0f) {
                    // Định kỳ 1 giây đánh giá lại mục tiêu xem có con mồi nào ngon hơn/gần hơn không
                    targetFood = null;
                    targetNavigator.clear();
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

                if (neighbor instanceof FoodSource) {
                    FoodSource foodSource = (FoodSource) neighbor;
                    // Tránh ăn thịt đồng loại
                    if (!canEatMeatSource(ownerAnimal, foodSource)) {
                        continue;
                    }
                    
                    // Điểm cơ bản cho nguồn thịt
                    score = 1000.0f - dist;
                    if (isVeryHungry) {
                        score += 5000.0f; // Rất đói thì ưu tiên nguồn thịt nhất
                    }
                    
                } else if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                    Animal other = (Animal) neighbor;
                    
                    // Không săn đồng loại, con cùng/cao cấp hơn, con mồi quá to, hoặc đang trốn
                    if (other.isHidden() || other.getSize() > ownerAnimal.getSize() * 1.5f 
                            || other.getSpeciesName().equals(ownerAnimal.getSpeciesName())
                            || other.getEntityLevel() >= ownerAnimal.getEntityLevel()) {
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
            if (targetFood instanceof FoodSource) {
                // Ăn thịt: giữ nguyên tầm ăn nhỏ, kết hợp với lực đẩy sẽ tạo ra hiệu ứng tranh giành mồi (chen lấn)
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
                        targetNavigator.clear();
                        ownerAnimal.setActionState("idle");
                    }
                } else if (targetFood instanceof FoodSource) {
                    ownerAnimal.setActionState("eat");
                    ownerAnimal.eatMeat((FoodSource) targetFood);

                    if (!targetFood.isAlive() || ownerAnimal.getHunger() >= ownerAnimal.getMaxHunger()) {
                        targetFood = null;
                        targetNavigator.clear();
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
                        targetNavigator.clear();
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
                
                targetNavigator.moveTo(ownerAnimal, world, targetFood.getPosition(), deltaTime,
                        interactRange, targetFood instanceof Animal ? 0.6f : 1.2f);
                if (targetNavigator.isBlocked()) {
                    targetFood = null;
                    ownerAnimal.setActionState("idle");
                }
            }
        } else {
            // Không có con mồi/thức ăn -> đi lang thang
            targetNavigator.clear();
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

    private boolean canEatMeatSource(Animal animal, FoodSource foodSource) {
        if (foodSource instanceof Carcass) {
            Carcass carcass = (Carcass) foodSource;
            return !carcass.getSourceSpecies().equals(animal.getSpeciesName());
        }
        return true;
    }
}

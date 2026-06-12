package model.strategies;

import core.GameConfig;
import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.living_beings.Human;
import model.living_beings.Hunter;
import model.items.Carcass;
import model.items.FireballProjectile;
import model.items.FoodSource;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.structures.Bush;
import model.world.World;
import model.entity.Entity;
import model.structures.FoodStorage;
import java.util.List;
import java.util.Random;

public class HunterStrategy implements IStrategy {
    private final PassiveStrategy wanderDelegate = new PassiveStrategy();
    private final PathNavigator targetNavigator = new PathNavigator();
    private final PathNavigator hunterRoamNavigator = new PathNavigator();
    private Entity targetFood = null;
    private Vector2 hunterRoamTarget = null;
    private float reassessTimer = 0f;
    private final Random random = new Random();
    private final GameConfig config = GameConfig.getInstance();
    private static final double RUN_COST_MULTIPLIER = 3.0;
    private static final float BUSH_CONCEALMENT_QUERY_RADIUS = 96.0f;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;
        if (ownerAnimal instanceof Human && ((Human) ownerAnimal).canHuntRole()) {
            executeHumanHunter((Human) ownerAnimal, world, deltaTime);
            return;
        }

        if (!ownerAnimal.canUseStrategy(HunterStrategy.class)) {
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

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
            if (!HunterTargeting.isTargetStillValid(ownerAnimal, targetFood, world)) {
                targetFood = null;
                targetNavigator.clear();
                ownerAnimal.setActionState("idle");
            } else if (targetFood instanceof Animal && reassessTimer > 1.0f) {
                // Định kỳ 1 giây đánh giá lại mục tiêu
                targetFood = null;
                targetNavigator.clear();
                reassessTimer = 0f;
            }
        }

        reassessTimer += deltaTime;

        // Quét tìm mục tiêu mới
        if (targetFood == null) {
            targetFood = HunterTargeting.findAnimalTarget(ownerAnimal, world);
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
                    HunterCombat.eatCarcass(ownerAnimal, (Carcass) targetFood, deltaTime);
                    
                    // Nếu ăn no hoặc xác biến mất
                    if (!targetFood.isAlive() || ownerAnimal.getHunger() >= ownerAnimal.getMaxHunger()) {
                        targetFood = null;
                        targetNavigator.clear();
                        ownerAnimal.setActionState("idle");
                    }
                } else if (targetFood instanceof FoodSource) {
                    ownerAnimal.setActionState("eat");
                    HunterCombat.eatMeat(ownerAnimal, (FoodSource) targetFood);

                    if (!targetFood.isAlive() || ownerAnimal.getHunger() >= ownerAnimal.getMaxHunger()) {
                        targetFood = null;
                        targetNavigator.clear();
                        ownerAnimal.setActionState("idle");
                    }
                } else if (targetFood instanceof Animal) {
                    ownerAnimal.setActionState("attack");
                    Animal prey = (Animal) targetFood;
                    
                    if (ownerAnimal instanceof model.living_beings.Fish) {
                        // CÁ SĂN MỒI NUỐT CHỬNG (Instant kill)
                        HunterCombat.instantKillFish(ownerAnimal, prey);
                        targetFood = null;
                        targetNavigator.clear();
                        ownerAnimal.setActionState("idle");
                    } else {
                        // THÚ TRÊN CẠN TẤN CÔNG TỪ TỪ
                        HunterCombat.attackAnimal(ownerAnimal, prey, deltaTime);

                        if (!prey.isAlive()) {
                            // Trả về null để lần sau quét trúng Carcass vừa rơi ra
                            targetFood = null;
                            targetNavigator.clear();
                            ownerAnimal.setActionState("idle");
                        }
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
                        interactRange, targetFood instanceof Animal ? 0.6f : 1.2f,
                        MovementContext.HUNTING);
                if (targetNavigator.isBlocked()) {
                    targetFood = null;
                    ownerAnimal.setActionState("idle");
                }
            }
        } else {
            // Không có con mồi/thức ăn -> đi lang thang
            targetNavigator.clear();
            ownerAnimal.setActionState("walk");
            if (ownerAnimal.getSpeed() != ownerAnimal.getBaseSpeed()) {
                ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            }
            wanderDelegate.execute(owner, world, deltaTime);
        }
    }



    private void executeHumanHunter(Human hunter, World world, float deltaTime) {
        if (world == null || world.getSpatialGrid() == null || !hunter.canUseStrategy(HunterStrategy.class)) {
            wanderDelegate.execute(hunter, world, deltaTime);
            return;
        }

        Hunter rangedHunter = hunter instanceof Hunter ? (Hunter) hunter : null;
        if (rangedHunter != null) {
            rangedHunter.tickFireCooldown(deltaTime);
        }

        if (shouldReturnToStorage(hunter, rangedHunter)) {
            depositCarriedFood(hunter, world, deltaTime);
            return;
        }

        if (rangedHunter != null && rangedHunter.needsAmmoReload()) {
            validateHumanHunterTarget(hunter);
            if (!(targetFood instanceof FoodSource)) {
                targetFood = HunterTargeting.findHumanHunterFoodTarget(hunter, world);
                targetNavigator.clear();
            }
            if (targetFood instanceof FoodSource) {
                moveToHumanHunterTarget(hunter, world, deltaTime);
                return;
            }
            reloadHunterAmmo(rangedHunter, world, deltaTime);
            return;
        }

        reassessTimer += deltaTime;
        validateHumanHunterTarget(hunter);

        if (targetFood == null || reassessTimer > 1.0f) {
            Entity newTarget = HunterTargeting.findHumanHunterTarget(hunter, world);
            if (newTarget != null) {
                targetFood = newTarget;
                targetNavigator.clear();
                hunterRoamNavigator.clear();
                hunterRoamTarget = null;
            }
            reassessTimer = 0f;
        }

        if (targetFood == null) {
            roamForPrey(hunter, world, deltaTime);
            return;
        }

        moveToHumanHunterTarget(hunter, world, deltaTime);
    }

    private void reloadHunterAmmo(Hunter hunter, World world, float deltaTime) {
        targetFood = null;
        targetNavigator.clear();
        hunterRoamNavigator.clear();
        hunterRoamTarget = null;

        if (hunter.isInHomeArea()) {
            hunter.setSpeed(0);
            hunter.setActionState("idle");
            hunter.reloadAtHome(deltaTime);
            return;
        }

        hunter.cancelReload();
        hunter.setActionState("run");
        hunter.setSpeed(hunter.getBaseSpeed() * 1.25f);
        Vector2 home = hunter.getHomeCenter();
        targetNavigator.moveTo(hunter, world, home, deltaTime, 32.0f, 1.0f, MovementContext.NORMAL);
        if (targetNavigator.isBlocked()) {
            targetNavigator.clear();
            hunter.setActionState("idle");
        }
    }

    private boolean shouldReturnToStorage(Human hunter, Hunter rangedHunter) {
        if (!hunter.hasCarriedFood()) return false;
        if (rangedHunter != null && rangedHunter.needsAmmoReload()) return true;
        return hunter.isCarryingFoodAtLeast(config.HUNTER_RETURN_FOOD_RATIO);
    }

    private void depositCarriedFood(Human hunter, World world, float deltaTime) {
        FoodStorage storage = findNearestHomeFoodStorage(hunter, world);
        if (storage == null) {
            hunter.setActionState("walk");
            wanderDelegate.execute(hunter, world, deltaTime);
            return;
        }

        float depositRange = hunter.getSize() / 2 + storage.getSize() / 2 + 20.0f;
        if (hunter.getPosition().distanceTo(storage.getPosition()) <= depositRange) {
            hunter.setSpeed(0);
            hunter.setActionState("idle");
            hunter.depositFood(storage);
            targetFood = null;
            targetNavigator.clear();
            return;
        }

        hunter.setActionState("walk");
        hunter.setSpeed(hunter.getBaseSpeed());
        Vector2 target = PathNavigator.findInteractionPoint(hunter, world, storage, depositRange);
        targetNavigator.moveTo(hunter, world, target, deltaTime, 8.0f, 1.0f, MovementContext.SEEKING_STRUCTURE);
        if (targetNavigator.isBlocked()) {
            targetNavigator.clear();
            hunter.setActionState("idle");
        }
    }

    private void validateHumanHunterTarget(Human hunter) {
        if (!HunterTargeting.isHumanTargetStillValid(hunter, targetFood)) {
            targetFood = null;
            targetNavigator.clear();
        }
    }

    private void moveToHumanHunterTarget(Human hunter, World world, float deltaTime) {
        Vector2 dirToTarget = targetFood.getPosition().copy().subtract(hunter.getPosition());
        float distToTarget = dirToTarget.length();
        if (dirToTarget.lengthSquared() > 0) dirToTarget.normalize();
        if (dirToTarget.x > 0) hunter.setFacingRight(true);
        else if (dirToTarget.x < 0) hunter.setFacingRight(false);

        if (targetFood instanceof Animal && hunter instanceof Hunter) {
            moveToRangedPrey((Hunter) hunter, world, (Animal) targetFood, dirToTarget, distToTarget, deltaTime);
            return;
        }

        float interactRange = hunter.getSize() / 2 + targetFood.getSize() / 2 + 5.0f;
        if (distToTarget <= interactRange) {
            hunter.setSpeed(0);
            if (targetFood instanceof FoodSource) {
                hunter.setActionState("eat");
                float needed = hunter.getCarryCapacity() - hunter.getCarriedFood();
                float collected = ((FoodSource) targetFood).consume(needed);
                hunter.addCarriedFood(collected);
                targetNavigator.clear();
                if (!targetFood.isAlive() || hunter.isCarryingFoodAtLeast(config.HUNTER_RETURN_FOOD_RATIO)) {
                    targetFood = null;
                }
            } else if (targetFood instanceof Animal) {
                hunter.setActionState("attack");
                Animal prey = (Animal) targetFood;
                HunterCombat.attackAnimal(hunter, prey, deltaTime);
                if (!prey.isAlive()) {
                    targetFood = null;
                    targetNavigator.clear();
                }
            }
            return;
        }

        if (targetFood instanceof Animal) {
            hunter.setActionState("run");
            hunter.setSpeed(hunter.getBaseSpeed() * 1.45f);
            double extraHunger = hunter.getHungerDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
            double extraThirst = hunter.getThirstDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
            hunter.setHunger(hunter.getHunger() - extraHunger);
            hunter.setThirst(hunter.getThirst() - extraThirst);
        } else {
            hunter.setActionState("walk");
            hunter.setSpeed(hunter.getBaseSpeed());
        }

        targetNavigator.moveTo(hunter, world, targetFood.getPosition(), deltaTime,
                interactRange, targetFood instanceof Animal ? 0.5f : 1.0f,
                MovementContext.HUNTING);
        if (targetNavigator.isBlocked()) {
            targetFood = null;
            targetNavigator.clear();
            hunter.setActionState("idle");
        }
    }

    private void moveToRangedPrey(Hunter hunter, World world, Animal prey, Vector2 dirToTarget,
                                  float distToTarget, float deltaTime) {
        if (!hunter.hasAmmo()) {
            targetFood = null;
            targetNavigator.clear();
            return;
        }

        float shootRange = config.HUNTER_SHOOT_RANGE;
        if (distToTarget <= shootRange) {
            hunter.setSpeed(0);
            hunter.setActionState("attack");
            targetNavigator.clear();
            if (hunter.canShoot()) {
                HunterCombat.shootFireball(hunter, world, prey, dirToTarget);
                hunter.consumeAmmo();
                hunter.resetFireCooldown();
            }
            return;
        }

        hunter.cancelReload();
        hunter.setActionState("run");
        hunter.setSpeed(hunter.getBaseSpeed() * 1.45f);
        double extraHunger = hunter.getHungerDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
        double extraThirst = hunter.getThirstDecayRate() * (RUN_COST_MULTIPLIER - 1) * deltaTime;
        hunter.setHunger(hunter.getHunger() - extraHunger);
        hunter.setThirst(hunter.getThirst() - extraThirst);

        targetNavigator.moveTo(hunter, world, prey.getPosition(), deltaTime,
                shootRange * 0.75f, 0.5f, MovementContext.HUNTING);
        if (targetNavigator.isBlocked()) {
            targetFood = null;
            targetNavigator.clear();
            hunter.setActionState("idle");
        }
    }



    private void roamForPrey(Human hunter, World world, float deltaTime) {
        if (hunterRoamTarget == null
                || hunter.getPosition().distanceTo(hunterRoamTarget) <= 24.0f
                || hunterRoamNavigator.isBlocked()) {
            hunterRoamTarget = randomHuntPoint(hunter, world);
            hunterRoamNavigator.clear();
        }

        if (hunterRoamTarget == null) {
            hunter.setActionState("walk");
            wanderDelegate.execute(hunter, world, deltaTime);
            return;
        }

        hunter.setActionState("walk");
        hunter.setSpeed(hunter.getBaseSpeed() * 0.9f);
        hunterRoamNavigator.moveTo(hunter, world, hunterRoamTarget, deltaTime, 24.0f, 2.0f);
    }

    private Vector2 randomHuntPoint(Human hunter, World world) {
        Vector2 center = hunter.getHomeCenter();
        float minRadius = hunter.getHomeRadius() * 0.7f;
        float maxRadius = hunter.getHomeRadius() * 2.4f;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = minRadius + random.nextDouble() * Math.max(1.0f, maxRadius - minRadius);
            Vector2 candidate = new Vector2(
                    (float) (center.x + Math.cos(angle) * radius),
                    (float) (center.y + Math.sin(angle) * radius)
            );
            float margin = hunter.getSize() / 2;
            candidate.x = Math.max(margin, Math.min(world.getWidth() - margin, candidate.x));
            candidate.y = Math.max(margin, Math.min(world.getHeight() - margin, candidate.y));
            if (world.isValidPositionFor(hunter, candidate)) return candidate;
        }
        return center;
    }

    private FoodStorage findNearestHomeFoodStorage(Human hunter, World world) {
        if (hunter.getHomeSettlement() != null) {
            return hunter.getHomeSettlement().findNearestFoodStorage(hunter.getPosition(), false);
        }
        List<Entity> candidates = world.getSpatialGrid() != null
                ? world.getSpatialGrid().getNeighbors(hunter.getHomeCenter(), hunter.getHomeRadius())
                : world.getEntities();
        FoodStorage best = null;
        float bestDist = Float.MAX_VALUE;
        for (Entity entity : candidates) {
            if (!(entity instanceof FoodStorage) || !entity.isAlive()) continue;
            if (!hunter.isInHomeArea(entity.getPosition())) continue;
            float dist = hunter.getPosition().distanceTo(entity.getPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = (FoodStorage) entity;
            }
        }
        return best;
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

    @Override
    public core.Vector2 getTarget() {
        if (targetFood != null) return targetFood.getPosition();
        return wanderDelegate.getTarget();
    }

    @Override
    public java.util.List<core.Vector2> getPath() {
        if (targetFood != null) return targetNavigator.getPath();
        return wanderDelegate.getPath();
    }
}

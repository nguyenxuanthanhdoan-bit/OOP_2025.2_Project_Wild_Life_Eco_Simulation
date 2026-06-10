package model.strategies;

import model.living_beings.Animal;
import model.living_beings.Human;

public final class StrategySelector {
    private StrategySelector() {}

    public static IStrategy select(Animal animal) {
        if (animal == null || !animal.isAliveState()) return null;

        boolean criticalHunger = animal.getHunger() < animal.getMaxHunger() * Animal.CRITICAL_SURVIVAL_THRESHOLD;
        boolean criticalThirst = animal.getThirst() < animal.getMaxThirst() * Animal.CRITICAL_SURVIVAL_THRESHOLD;
        boolean hungry = animal.getHunger() < animal.getMaxHunger() * Animal.HUNGER_WARNING_THRESHOLD;
        boolean thirsty = animal.getThirst() < animal.getMaxThirst() * Animal.THIRST_WARNING_THRESHOLD;

        if (criticalThirst) {
            return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
        }

        boolean isNight = animal.getWorld() != null &&
                          (animal.getWorld().getTimeOfDay() >= 18.0f || animal.getWorld().getTimeOfDay() <= 5.0f);
        boolean isAquatic = animal.getProfile().isAquatic();
        boolean isNocturnal = animal.getProfile().isNocturnal();

        // [MỚI] Động vật (kể cả thú ăn thịt) sẽ bỏ chạy nếu thấy người gần vườn
        if (!isAquatic && !(animal instanceof Human) && animal.hasGardenThreat()) {
            return currentOrNew(animal, ScaredStrategy.class, new ScaredStrategy());
        }

        if (animal.hasDangerousThreats() && animal.canUseStrategy(ScaredStrategy.class)) {
            return currentOrNew(animal, ScaredStrategy.class, new ScaredStrategy());
        }

        if (animal instanceof Human) {
            Human human = (Human) animal;

            // =============================================
            // AI GOAL SYSTEM CHO HUMAN
            // Con người là thực thể tối cao — không có kẻ thù.
            // Priority: GO_HOME (ban đêm) > WANDER (ban ngày)
            // =============================================

            // 1. GO_HOME / SLEEP: Ban đêm → về nhà ngủ
            if (isNight && !isAquatic) {
                if (human.isVillager()) {
                    return currentOrNew(animal, GoHomeStrategy.class, new GoHomeStrategy());
                }
                // Hunter ban đêm: săn nếu cần, còn không thì về nhà
                if (human.isHunter()) {
                    if (criticalHunger || hungry || human.shouldHuntForVillage()) {
                        return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
                    }
                    return currentOrNew(animal, GoHomeStrategy.class, new GoHomeStrategy());
                }
            }


            // 2. Phân công công việc ban ngày theo giới tính
            if (!isNight && animal.getWorld() != null) {
                if (human.getVariant() == Human.Variant.FEMALE) {
                    // Phụ nữ ưu tiên thu hoạch chậu hoa trưởng thành
                    if (animal.getCurrentStrategy() instanceof HarvestStrategy) {
                        return animal.getCurrentStrategy();
                    }
                    model.structures.GardenBed bed = animal.getWorld().getCropManager().findNearestMatureCrop(animal.getPosition());
                    if (bed != null) {
                        return new HarvestStrategy(bed);
                    }
                } else if (human.getVariant() == Human.Variant.MALE) {
                    if (human.getRole() == model.living_beings.HumanRole.FISHERMAN) {
                        // Đàn ông đi chài lưới
                        if (animal.getCurrentStrategy() instanceof BoardBoatStrategy) {
                            return animal.getCurrentStrategy();
                        }
                        
                        // Tìm thuyền đang neo bến
                        model.structures.Boat targetBoat = null;
                        for (model.entity.Entity e : animal.getWorld().getEntities()) {
                            if (e instanceof model.structures.Boat) {
                                model.structures.Boat b = (model.structures.Boat) e;
                                if (b.canBoard() && b.getPosition().distanceTo(animal.getPosition()) < 1500f) {
                                    targetBoat = b;
                                    break;
                                }
                            }
                        }
                        
                        if (targetBoat != null) {
                            return new BoardBoatStrategy(targetBoat);
                        }
                    }
                }
            }

            // 3. Logic Hunter ban ngày
            if (human.isHunter() && shouldHunterReturnFood(human)) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            }
            if (human.isHunter() && (criticalHunger || hungry || human.shouldHuntForVillage())) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            }

            // 4. WANDER: Ban ngày → lang thang trong homeArea
            if (criticalThirst) {
                return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
            }
            if (hungry && human.canForageForFood()) {
                return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
            }
            if (thirsty) {
                return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
            }
            if (human.canReproduce()) {
                return currentOrNew(animal, MatingStrategy.class, new MatingStrategy());
            }
            return currentOrNew(animal, PassiveStrategy.class, new PassiveStrategy());
        }

        // =============================================
        // AI CHO CÁC LOÀI ĐỘNG VẬT KHÁC (không phải Human)
        // =============================================

        boolean predatorHungry = animal.getDietType() == model.living_beings.DietType.CARNIVORE &&
                                 animal.getHunger() < animal.getMaxHunger() * 0.90;

        if ((criticalHunger || hungry || predatorHungry) && animal.canUseStrategy(HunterStrategy.class)) {
            return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
        }

        if (isNight && !isNocturnal && !isAquatic && !criticalHunger) {
            return currentOrNew(animal, SleepStrategy.class, new SleepStrategy());
        }

        if (hungry && animal.canUseStrategy(ForageStrategy.class) && animal.canForageForFood()) {
            return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
        }

        if (thirsty) {
            return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
        }

        if (animal.canReproduce()) {
            return currentOrNew(animal, MatingStrategy.class, new MatingStrategy());
        }

        if (animal.canUseStrategy(FlockingStrategy.class)) {
            return currentOrNew(animal, FlockingStrategy.class, new FlockingStrategy());
        }

        return currentOrNew(animal, PassiveStrategy.class, new PassiveStrategy());
    }

    private static IStrategy currentOrNew(Animal animal, Class<? extends IStrategy> strategyType, IStrategy fallback) {
        IStrategy current = animal.getCurrentStrategy();
        if (strategyType.isInstance(current)) {
            return current;
        }
        return fallback;
    }

    private static boolean shouldHunterReturnFood(Human human) {
        if (!human.hasCarriedFood()) return false;
        if (human instanceof model.living_beings.Hunter
                && ((model.living_beings.Hunter) human).needsAmmoReload()) {
            return true;
        }
        return human.isCarryingFoodAtLeast(core.GameConfig.getInstance().HUNTER_RETURN_FOOD_RATIO);
    }
}

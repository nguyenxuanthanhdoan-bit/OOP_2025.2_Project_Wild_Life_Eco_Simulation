package model.strategies;

import model.living_beings.Animal;
import model.living_beings.Human;

public final class StrategySelector {
    private StrategySelector() {}

    public static IStrategy select(Animal animal) {
        if (animal == null || !animal.isAliveState()) return null;

        IStrategy current = animal.getCurrentStrategy();
        if (current != null && current.isInNonInterruptiblePhase()) {
            return current;
        }

        boolean criticalHunger = animal.getHunger() < animal.getMaxHunger() * Animal.CRITICAL_SURVIVAL_THRESHOLD;
        boolean criticalThirst = animal.getThirst() < animal.getMaxThirst() * Animal.CRITICAL_SURVIVAL_THRESHOLD;
        boolean hungry = animal.getHunger() < animal.getMaxHunger() * Animal.HUNGER_WARNING_THRESHOLD;
        boolean thirsty = animal.getThirst() < animal.getMaxThirst() * Animal.THIRST_WARNING_THRESHOLD;

        if (criticalThirst) {
            return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
        }

        // Ưu tiên cao nhất cho Cơn đói cực hạn (Sắp chết đói)
        if (criticalHunger) {
            if (animal.canUseStrategy(HunterStrategy.class)) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            } else if (animal.canUseStrategy(ForageStrategy.class) && animal.canForageForFood()) {
                return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
            }
        }

        boolean isNight = animal.getWorld() != null &&
                          (animal.getWorld().getTimeOfDay() >= 18.0f || animal.getWorld().getTimeOfDay() <= 5.0f);
        boolean isAquatic = animal.getProfile().isAquatic();
        boolean isNocturnal = animal.getProfile().isNocturnal();

        if (!isAquatic && animal.hasGardenThreat()) {
            return currentOrNew(animal, ScaredStrategy.class, new ScaredStrategy());
        }

        if (animal.hasDangerousThreats() && animal.canUseStrategy(ScaredStrategy.class)) {
            return currentOrNew(animal, ScaredStrategy.class, new ScaredStrategy());
        }

        if (animal instanceof Human) {
            Human human = (Human) animal;

            if (current != null && current.isCommittedTask()
                    && !current.shouldInterrupt(animal, animal.getWorld())) {
                return current;
            }

            // 3. Hunter đang mang thịt/hết đạn → về kho
            if (human.canHuntRole() && shouldHunterReturnFood(human)) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            }

            // 4. Ban đêm: tất cả canUseHouse() về nhà ngủ
            if (isNight && human.canUseHouse()) {
                if (human.canHuntRole()) {
                    // Hunter ban đêm: săn khi cần, còn không thì về nhà
                    if (hungry || human.canHuntForVillage()) {
                        return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
                    }
                }
                return currentOrNew(animal, GoHomeStrategy.class, new GoHomeStrategy());
            }

            // 5. Đói / khát thông thường
            if (hungry && human.canForageForFood()) {
                return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
            }
            if (thirsty) {
                return currentOrNew(animal, ForageStrategy.class, new ForageStrategy());
            }

            // 6. Hunter ban ngày: săn mồi / về kho
            if (human.canHuntRole() && (hungry || human.canHuntForVillage())) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            }

            // 7. Sinh sản (Villager và Fisherman)
            if (human.canReproduce()) {
                return currentOrNew(animal, MatingStrategy.class, new MatingStrategy());
            }

            // 8. Công việc theo capability của role (ban ngày)
            if (!isNight && animal.getWorld() != null) {
                // Harvest: giữ strategy nếu đang làm dở
                if (human.canHarvest()) {
                    model.structures.GardenBed bed = animal.getWorld().getCropManager()
                            .reserveNearestMatureCrop(human);
                    if (bed != null) {
                        return new HarvestStrategy(bed);
                    }
                }

                if (human.canFish()) {
                    model.structures.Boat targetBoat = animal.getWorld().getCoastalManager()
                            .reserveAvailableBoat(human);
                    if (targetBoat != null) {
                        return new BoardBoatStrategy(targetBoat);
                    }
                }
            }

            // 9. Passive — lang thang trong homeArea
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

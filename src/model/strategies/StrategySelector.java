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

        if (animal.hasDangerousThreats() && animal.canUseStrategy(ScaredStrategy.class)) {
            return currentOrNew(animal, ScaredStrategy.class, new ScaredStrategy());
        }

        if (animal instanceof Human) {
            Human human = (Human) animal;
            if (human.isHunter() && shouldHunterReturnFood(human)) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            }
            if (human.isHunter() && (criticalHunger || hungry || human.shouldHuntForVillage())) {
                return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
            }
        }

        if ((criticalHunger || hungry) && animal.canUseStrategy(HunterStrategy.class)) {
            return currentOrNew(animal, HunterStrategy.class, new HunterStrategy());
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

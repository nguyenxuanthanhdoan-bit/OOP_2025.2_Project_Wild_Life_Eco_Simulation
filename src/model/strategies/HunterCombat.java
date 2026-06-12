package model.strategies;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.items.Carcass;
import model.items.FireballProjectile;
import model.items.FoodSource;
import model.living_beings.Animal;
import model.living_beings.Hunter;
import model.world.World;

public class HunterCombat {
    private static final GameConfig config = GameConfig.getInstance();

    public static void attackAnimal(Animal attacker, Animal prey, float deltaTime) {
        prey.takeDamage(attacker.getProfile().getAttackDamagePerSecond() * deltaTime);
    }

    public static void instantKillFish(Animal attacker, Animal prey) {
        float nutrition = (float) prey.getMaxHunger();
        attacker.setHunger(Math.min(attacker.getMaxHunger(), attacker.getHunger() + nutrition));
        prey.takeDamage(prey.getHealth() + 9999); // Giết ngay lập tức
    }

    public static void eatCarcass(Animal eater, Carcass carcass, float deltaTime) {
        eater.eatCarcass(carcass, deltaTime);
    }

    public static void eatMeat(Animal eater, FoodSource food) {
        eater.eatMeat(food);
    }

    public static void shootFireball(Hunter hunter, World world, Animal prey, Vector2 direction) {
        Vector2 shotDir = direction.copy();
        if (shotDir.lengthSquared() <= Vector2.EPSILON) {
            shotDir.set(hunter.isFacingRight() ? 1.0f : -1.0f, 0.0f);
        } else {
            shotDir.normalize();
        }

        Vector2 start = hunter.getPosition().copy()
                .add(shotDir.copy().scale(hunter.getSize() / 2.0f + config.HUNTER_PROJECTILE_SIZE / 2.0f + 2.0f));
        world.addEntity(new FireballProjectile(start, prey, config.HUNTER_PROJECTILE_SPEED,
                config.HUNTER_PROJECTILE_DAMAGE, config.HUNTER_PROJECTILE_SIZE));
    }
}

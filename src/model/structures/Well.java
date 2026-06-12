package model.structures;

import core.GameConfig;
import core.Vector2;
import model.entity.Structure;
import model.living_beings.animal.Animal;

/**
 * Giếng nước trong village.
 * Hiện tại là nguồn nước tĩnh để chuẩn bị cho Human/Villager.
 */
public class Well extends Structure {
    private static final float DRINK_RADIUS = 34.0f;

    private final float drinkRadius;

    public Well(Vector2 position, int variant) {
        super(position, GameConfig.getInstance().WELL_SIZE, "WELL", "well_" + clampVariant(variant, 1, 2), true);
        this.drinkRadius = DRINK_RADIUS;
    }

    public boolean isInDrinkRange(Vector2 pos) {
        return pos != null && this.position.distanceTo(pos) <= drinkRadius;
    }

    public boolean drink(Animal animal) {
        if (animal == null || !animal.isAlive() || !isInDrinkRange(animal.getPosition())) {
            return false;
        }
        animal.setThirst(animal.getMaxThirst());
        return true;
    }

    public float getDrinkRadius() {
        return drinkRadius;
    }

    private static int clampVariant(int variant, int min, int max) {
        return Math.max(min, Math.min(max, variant));
    }
}

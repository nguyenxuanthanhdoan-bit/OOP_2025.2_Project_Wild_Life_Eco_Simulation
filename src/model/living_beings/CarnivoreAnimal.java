package model.living_beings;

import core.Vector2;
import model.living_beings.animal.Animal;

/**
 * Lớp cơ sở cho các loài động vật ăn thịt.
 * Phiên bản Phase 1: Tối giản, chỉ làm cầu nối kế thừa từ Animal.
 */
public abstract class CarnivoreAnimal extends Animal {

    public CarnivoreAnimal(Vector2 position, float size, float baseSpeed) {
        super(position, size, baseSpeed, DietType.CARNIVORE);
    }
}

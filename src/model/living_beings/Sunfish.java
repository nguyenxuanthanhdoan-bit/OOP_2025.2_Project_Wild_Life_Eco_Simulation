package model.living_beings;

import core.Vector2;

public class Sunfish extends Fish {
    public Sunfish(Vector2 position, HabitatRule rule) {
        super(position, 32f, 40f, "Sunfish", 80, 80, 1.0, 100, 0, 80, 300, DietType.HERBIVORE, rule);
    }

    @Override
    public Animal reproduce() {
        return new Sunfish(this.position.copy(), this.habitatRule);
    }
}

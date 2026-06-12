package model.living_beings;

import core.Vector2;

public class Clownfish extends Fish {
    public Clownfish(Vector2 position, HabitatRule rule) {
        super(position, 18f, 40f, "Clownfish", 30, 40, 0.6, 100, 0, 45, 180, DietType.HERBIVORE, rule);
    }

    @Override
    public Animal reproduce() {
        return new Clownfish(this.position.copy(), this.habitatRule);
    }
}

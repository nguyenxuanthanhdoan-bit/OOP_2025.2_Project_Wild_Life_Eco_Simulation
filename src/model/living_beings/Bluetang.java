package model.living_beings;

import model.living_beings.animal.Animal;

import core.Vector2;

public class Bluetang extends Fish {
    public Bluetang(Vector2 position, HabitatRule rule) {
        super(position, 20f, 45f, "Bluetang", 40, 50, 0.7, 100, 0, 50, 225, DietType.HERBIVORE, rule);
    }

    @Override
    public Animal reproduce() {
        return new Bluetang(this.position.copy(), this.habitatRule);
    }
}

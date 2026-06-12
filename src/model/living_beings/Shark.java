package model.living_beings;

import model.living_beings.animal.Animal;

import core.Vector2;

public class Shark extends Fish {
    public Shark(Vector2 position, HabitatRule rule) {
        super(position, 48f, 60f, "Shark", 200, 150, 1.5, 100, 0, 100, 450, DietType.CARNIVORE, rule);
    }

    @Override
    public Animal reproduce() {
        return new Shark(this.position.copy(), this.habitatRule);
    }
}

package model.living_beings;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Plant;
import model.strategies.PassiveStrategy;
import java.util.List;
import java.util.Random;

public class Deer extends HerbivoreAnimal {
    private static final float  SIZE              = 30.0f;
    private static final float  BASE_SPEED        = 130.0f;
    private int herdId = 1;
    private static final double MAX_HEALTH        = 150.0;
    private static final double MAX_HUNGER        = 150.0;
    private static final double HUNGER_DECAY_RATE = 2.5;
    private static final double MAX_THIRST        = 130.0;
    private static final double THIRST_DECAY_RATE = 2.0;
    private static final double MAX_AGE           = 900.0;
    private static final double VISION_RANGE      = 280.0;

    private final Random random = new Random();

    public Deer(Vector2 position, int herdId) {
        this(position);
        this.herdId = herdId;
    }

    public Deer(Vector2 position) {
        super(position, SIZE, BASE_SPEED);
        this.speciesName = "Hươu";
        this.maxHealth        = MAX_HEALTH;
        this.health           = MAX_HEALTH;
        this.maxHunger        = MAX_HUNGER;
        this.hunger           = MAX_HUNGER;
        this.hungerDecayRate  = HUNGER_DECAY_RATE;
        this.maxThirst        = MAX_THIRST;
        this.thirst           = MAX_THIRST;
        this.thirstDecayRate  = THIRST_DECAY_RATE;
        this.maxAge           = MAX_AGE;
        this.visionRange      = VISION_RANGE;

        this.setStrategy(new PassiveStrategy());
    }

    @Override
    public Animal reproduce() {
        return null;
    }

    @Override
    public void eat(Plant food) {
        if (!alive) return;
        if (food instanceof Grass || food instanceof Fruit) {
            super.eat(food);
        }
    }

    @Override
    public void render(DisplayMode mode) {}
}

package model.living_beings;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.plants.Grass;
import model.plants.Plant;
import model.strategies.PassiveStrategy;
import java.util.List;
import java.util.Random;

public class Rabbit extends HerbivoreAnimal {
    private static final float  SIZE              = 20.0f;
    private static final float  BASE_SPEED        = GameConfig.getInstance().RABBIT_BASE_SPEED;
    private static final double MAX_HEALTH        = 50.0;
    private static final double MAX_HUNGER        = 100.0;
    private static final double HUNGER_DECAY_RATE = 2.0;
    private static final double MAX_THIRST        = 100.0;
    private static final double THIRST_DECAY_RATE = 3.0;
    private static final double MAX_AGE           = 300.0;
    private static final double VISION_RANGE      = 200.0;

    private final Random random = new Random();

    public Rabbit(Vector2 position) {
        super(position, SIZE, BASE_SPEED);
        this.speciesName = "Thỏ";
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
        if (food instanceof Grass) {
            super.eat(food);
        }
    }

    @Override
    public void render(DisplayMode mode) {}
}
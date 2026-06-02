package model.living_beings;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Plant;
import model.strategies.FlockingStrategy;
import model.strategies.PassiveStrategy;
import java.util.Random;

public class Elephant extends HerbivoreAnimal {
    private static final float  SIZE               = 50.0f;
    private static final float  BASE_SPEED         = 60.0f;
    private int herdId = 1;
    private static final double MAX_HEALTH         = 500.0;
    private static final double MAX_HUNGER         = 300.0;
    private static final double HUNGER_DECAY_RATE  = 3.0;
    private static final double MAX_THIRST         = 250.0;
    private static final double THIRST_DECAY_RATE  = 2.5;
    private static final double MAX_AGE            = 3600.0;
    private static final double VISION_RANGE       = 350.0;

    private final Random random = new Random();

    public Elephant(Vector2 position, int herdId) {
        this(position);
        this.herdId = herdId;
    }

    public Elephant(Vector2 position) {
        super(position, SIZE, BASE_SPEED);
        this.speciesName = "Voi";
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

        this.setStrategy(new FlockingStrategy.ElephantFlock());
    }

    @Override
    public Animal reproduce() {
        Elephant baby = new Elephant(this.getPosition().copy());
        baby.age = 0;
        baby.size = SIZE * 0.5f; // 50% kích thước
        baby.setAdult(false);
        // Con non chỉ sử dụng PassiveStrategy
        baby.setStrategy(new PassiveStrategy());
        return baby;
    }

    @Override
    public void eat(Plant food) {
        if (!alive) return;
        if (food instanceof Grass || food instanceof Fruit) {
            super.eat(food);
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // Cập nhật strategy khi trưởng thành
        if (this.adult && this.currentStrategy instanceof PassiveStrategy && this.age > 0) {
            this.setStrategy(new FlockingStrategy.ElephantFlock());
            this.size = SIZE;
        }
    }

    @Override
    public void render(DisplayMode mode) {}
}

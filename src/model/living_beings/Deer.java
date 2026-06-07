package model.living_beings;

import core.DisplayMode;
import core.Vector2;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Mushroom;
import model.plants.Plant;
import model.strategies.FlockingStrategy;

public class Deer extends HerbivoreAnimal {
    private static final float  SIZE              = 30.0f;
    private static final float  BASE_SPEED        = 130.0f;
    private int herdId = 1;
    private static final double MAX_HEALTH        = 150.0;
    private static final double MAX_HUNGER        = 150.0;
    private static final double HUNGER_DECAY_RATE = 0.6;
    private static final double MAX_THIRST        = 130.0;
    private static final double THIRST_DECAY_RATE = 0.5;
    private static final double MAX_AGE           = 900.0;
    private static final double VISION_RANGE      = 280.0;

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
        // Không set cứng Strategy — để decideActiveStrategy tự quyết định
    }

    @Override
    protected boolean useFlocking() {
        return true; // Hươu đi theo bầy
    }

    @Override
    protected FlockingStrategy createFlockingStrategy() {
        return new FlockingStrategy.DeerFlock();
    }

    @Override
    public Animal reproduce() {
        Deer baby = new Deer(this.getPosition().copy());
        baby.age = 0;
        baby.size = SIZE * 0.5f; // 50% kích thước
        baby.setAdult(false);
        // Không set cứng
        return baby;
    }

    @Override
    protected model.items.Carcass createCarcass() {
        // Giảm kích thước hiển thị của miếng thịt từ 20.0f xuống 12.0f
        return new model.items.Carcass(this.position.copy(), 12.0f, 80.0f, 90.0f, 80.0f, "Hươu");
    }

    @Override
    public void eat(Plant food) {
        if (!alive) return;
        super.eat(food);
    }

    @Override
    public boolean canEatPlant(Plant food) {
        return food instanceof Grass || food instanceof Fruit || food instanceof Mushroom;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // Cập nhật kích thước khi trưởng thành, không reset strategy mỗi frame.
        if (this.adult && this.age > 0) {
            this.size = SIZE;
        }
    }

    @Override
    public void render(DisplayMode mode) {}
}

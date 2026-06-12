package model.living_beings;

import model.living_beings.animal.Animal;

import core.DisplayMode;
import core.Vector2;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Mushroom;
import model.plants.Plant;

public class Elephant extends HerbivoreAnimal {
    private static final float  SIZE               = 50.0f;
    private static final float  BASE_SPEED         = 25.0f;
    private int herdId = 1;
    private static final double MAX_HEALTH         = 500.0;
    private static final double MAX_HUNGER         = 300.0;
    private static final double HUNGER_DECAY_RATE  = 0.8;
    private static final double MAX_THIRST         = 250.0;
    private static final double THIRST_DECAY_RATE  = 0.6;
    private static final double MAX_AGE            = 3600.0;
    private static final double VISION_RANGE       = 350.0;
    private static final AnimalProfile PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_APEX_ANIMAL)
            .ediblePlants(Grass.class, Fruit.class, Mushroom.class)
            .canFlock(true)
            .flockingMode(FlockingMode.GUARDIAN)
            .canBeScared(false)
            .build();

    public Elephant(Vector2 position, int herdId) {
        this(position);
        this.herdId = herdId;
    }

    public Elephant(Vector2 position) {
        super(position, SIZE, BASE_SPEED);
        this.speciesName = "Voi";
        this.setMaxHealth(MAX_HEALTH);
        this.setHealth(MAX_HEALTH);
        this.setMaxHunger(MAX_HUNGER);
        this.setHunger(MAX_HUNGER);
        this.setHungerDecayRate(HUNGER_DECAY_RATE);
        this.setMaxThirst(MAX_THIRST);
        this.setThirst(MAX_THIRST);
        this.setThirstDecayRate(THIRST_DECAY_RATE);
        this.setMaxAge(MAX_AGE);
        this.setVisionRange(VISION_RANGE);
        this.profile          = PROFILE;
        // Không set cứng Strategy — để decideActiveStrategy tự quyết định
    }

    @Override
    public int getEntityLevel() {
        return LEVEL_APEX_ANIMAL;
    }

    @Override
    public Animal reproduce() {
        Elephant baby = new Elephant(this.getPosition().copy());
        baby.setAge(0);
        baby.size = SIZE * 0.5f; // 50% kích thước
        baby.setAdult(false);
        // Không set cứng
        return baby;
    }

    @Override
    protected model.items.Carcass createCarcass() {
        return new model.items.Carcass(this.position.copy(), 50.0f, 300.0f, 240.0f, 300.0f, "Voi");
    }

    @Override
    public void eat(Plant food) {
        if (!alive) return;
        super.eat(food);
    }

    @Override
    public boolean canEatPlant(Plant food) {
        return super.canEatPlant(food);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // Cập nhật kích thước khi trưởng thành, không reset strategy mỗi frame.
        if (this.isAdult() && this.getAge() > 0) {
            this.size = SIZE;
        }
    }

    @Override
    public void render(DisplayMode mode) {}
}

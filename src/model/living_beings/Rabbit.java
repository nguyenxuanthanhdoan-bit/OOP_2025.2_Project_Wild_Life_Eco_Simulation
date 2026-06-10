package model.living_beings;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.plants.Grass;
import model.plants.Plant;
import java.util.Random;

public class Rabbit extends HerbivoreAnimal {
    private static final float  SIZE              = 20.0f;
    private static final float  BASE_SPEED        = GameConfig.getInstance().RABBIT_BASE_SPEED;
    private static final double MAX_HEALTH        = 50.0;
    private static final double MAX_HUNGER        = 100.0;
    private static final double HUNGER_DECAY_RATE = 0.5;
    private static final double MAX_THIRST        = 100.0;
    private static final double THIRST_DECAY_RATE = 0.8;
    private static final double MAX_AGE           = 900.0; // 20% = 180s = 3 phút để trưởng thành
    private static final double VISION_RANGE      = 200.0;
    private static final AnimalProfile PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_HERBIVORE)
            .ediblePlants(Grass.class)
            .canFlock(true)
            .flockingMode(FlockingMode.BASIC)
            .canHide(true)
            .canBeScared(true)
            .build();

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
        this.profile          = PROFILE;
        // Không set cứng Strategy — để decideActiveStrategy tự quyết định
    }

    @Override
    public Animal reproduce() {
        Rabbit baby = new Rabbit(this.getPosition().copy());
        baby.setAge(0);
        // Kích thước thỏ con bằng 50% thỏ trưởng thành
        baby.size = SIZE * 0.5f; 
        baby.setAdult(false);
        // Không set cứng
        return baby;
    }

    protected model.items.Carcass createCarcass() {
        return new model.items.Carcass(this.position.copy(), 20.0f, 40.0f, 60.0f, 40.0f, "Thỏ");
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
        updateAnimation();
    }

    private void updateAnimation() {
        if ("attack".equals(this.actionState)) {
            this.imageVariant = "west.png"; // No attack frame for rabbit, fallback to west
        } else if (this.isMoving && ("run".equals(this.actionState) || this.speed > this.baseSpeed * 1.1f)) {
            this.imageVariant = "run.png";
        } else if (this.isMoving) {
            this.imageVariant = "walk.png";
        } else {
            this.imageVariant = "west.png"; // idle
        }
    }

    @Override
    public void render(DisplayMode mode) {}
}

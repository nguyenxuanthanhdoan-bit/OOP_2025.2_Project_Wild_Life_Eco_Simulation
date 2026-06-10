package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;

public class Tiger extends CarnivoreAnimal {
    private static final float SIZE = 35.0f;
    private static final float BASE_SPEED = GameConfig.getInstance().TIGER_BASE_SPEED;
    private static final AnimalProfile PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_CARNIVORE)
            .canHunt(true)
            .canEatMeat(true)
            .attackDamagePerSecond(100.0f)
            .maxPreySizeMultiplier(1.5f)
            .avoidsGuardedGardens(true)
            .settlementPolicy(AnimalProfile.SettlementPolicy.AVOID)
            .build();

    public Tiger(Vector2 position) {
        super(position, SIZE, BASE_SPEED);

        this.speciesName = "Hổ";
        this.maxHealth = 250.0f;
        this.health = this.maxHealth;
        this.maxHunger = 200.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 0.4f; 
        this.maxThirst = 150.0f;
        this.visionRange = 450.0;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 0.5f; 
        this.maxAge = 600.0f; 
        this.profile = PROFILE;

        // Không set strategy cứng — decideActiveStrategy() sẽ tự quyết định
    }

    @Override
    public Animal reproduce() {
        Tiger baby = new Tiger(this.getPosition().copy());
        baby.setAge(0);
        baby.size = SIZE * 0.5f;
        baby.setAdult(false);
        return baby;
    }

    @Override
    protected model.items.Carcass createCarcass() {
        return new model.items.Carcass(this.position.copy(), 30.0f, 120.0f, 150.0f, 120.0f, "Hổ");
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        updateAnimation();
    }

    private void updateAnimation() {
        if ("attack".equals(this.actionState)) {
            this.imageVariant = "attack.png";
        } else if (this.isMoving && ("run".equals(this.actionState) || this.speed > this.baseSpeed * 1.1f)) {
            this.imageVariant = "run.png";
        } else if (this.isMoving) {
            this.imageVariant = "walk.png";
        } else {
            this.imageVariant = "west.png";
        }
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

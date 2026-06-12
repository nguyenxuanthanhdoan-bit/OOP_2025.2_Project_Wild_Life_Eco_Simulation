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
            .canBeScared(true)
            .attackDamagePerSecond(100.0f)
            .maxPreySizeMultiplier(1.5f)
            .avoidsGuardedGardens(true)
            .settlementPolicy(AnimalProfile.SettlementPolicy.AVOID)
            .build();

    public Tiger(Vector2 position) {
        super(position, SIZE, BASE_SPEED);

        this.speciesName = "Hổ";
        this.setMaxHealth(260.0f);
        this.setHealth(260.0f);
        this.setMaxHunger(200.0f);
        this.setHunger(200.0f);
        this.setHungerDecayRate(0.4f); 
        this.setMaxThirst(150.0f);
        this.setVisionRange(360.0);
        this.setThirst(150.0f);
        this.setThirstDecayRate(0.5f); 
        this.setMaxAge(600.0f); 
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
        String animationState = getAnimationState();
        if ("attack".equals(animationState)) {
            this.imageVariant = "attack.png";
        } else if ("run".equals(animationState)) {
            this.imageVariant = "run.png";
        } else if ("walk".equals(animationState)) {
            this.imageVariant = "walk.png";
        } else {
            this.imageVariant = "west.png";
        }
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

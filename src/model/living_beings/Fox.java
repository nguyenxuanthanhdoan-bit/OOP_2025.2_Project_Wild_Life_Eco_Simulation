package model.living_beings;

import core.Vector2;
import core.DisplayMode;

public class Fox extends CarnivoreAnimal {
    private static final float SIZE = 24.0f;
    private static final float BASE_SPEED = 90.0f;
    private static final AnimalProfile PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_CARNIVORE)
            .canHunt(true)
            .canEatMeat(true)
            .canBeScared(true)
            .attackDamagePerSecond(50.0f)
            .maxPreySizeMultiplier(1.2f)
            .avoidsGuardedGardens(true)
            .settlementPolicy(AnimalProfile.SettlementPolicy.AVOID)
            .isDesertAdapted(true)
            .build();

    public Fox(Vector2 position) {
        super(position, SIZE, BASE_SPEED);

        this.speciesName = "Fox";
        this.setMaxHealth(100.0f);
        this.setHealth(100.0f);
        this.setMaxHunger(100.0f);
        this.setHunger(100.0f);
        this.setHungerDecayRate(0.4f);
        this.setMaxThirst(80.0f);
        this.setVisionRange(319.2);
        this.setThirst(80.0f);
        this.setThirstDecayRate(0.5f);
        this.setMaxAge(400.0f);
        this.profile = PROFILE;
    }

    @Override
    public Animal reproduce() {
        Fox baby = new Fox(this.getPosition().copy());
        baby.setAge(0);
        baby.size = SIZE * 0.5f;
        baby.setAdult(false);
        return baby;
    }

    @Override
    protected model.items.Carcass createCarcass() {
        return new model.items.Carcass(this.position.copy(), 20.0f, 60.0f, 80.0f, 80.0f, "Fox");
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        updateAnimation();
    }

    private void updateAnimation() {
        String animationState = getAnimationState();
        if ("sleep".equals(animationState)) {
            this.imageVariant = "sleep.png";
        } else if ("attack".equals(animationState)) {
            this.imageVariant = "west.png"; // Chưa có frame attack riêng
        } else if ("eat".equals(animationState) || "drink".equals(animationState)) {
            this.imageVariant = "eat.png";
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

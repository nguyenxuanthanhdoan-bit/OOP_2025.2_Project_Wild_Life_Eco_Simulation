package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;

public class Wolf extends CarnivoreAnimal {
    private static final float SIZE = 28.0f;
    private static final float BASE_SPEED = GameConfig.getInstance().WOLF_BASE_SPEED;
    private static final AnimalProfile PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_CARNIVORE)
            .canHunt(true)
            .canEatMeat(true)
            .canBeScared(true)
            .attackDamagePerSecond(80.0f)
            .maxPreySizeMultiplier(1.5f)
            .avoidsGuardedGardens(true)
            .settlementPolicy(AnimalProfile.SettlementPolicy.AVOID)
            .build();

    public Wolf(Vector2 position) {
        super(position, SIZE, BASE_SPEED);

        this.speciesName = "Sói";
        this.maxHealth = 160.0f;
        this.health = this.maxHealth;
        this.maxHunger = 120.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 0.5f;
        this.maxThirst = 100.0f;
        this.visionRange = 300.0;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 0.6f;
        this.maxAge = 450.0f;
        this.profile = PROFILE;

        // Không set strategy cứng — decideActiveStrategy() sẽ tự quyết định
        // Sói đi săn khi đói (HunterStrategy), đi dạo/bầy đàn khi no
    }

    @Override
    public Animal reproduce() {
        Wolf baby = new Wolf(this.getPosition().copy());
        baby.setAge(0);
        baby.size = SIZE * 0.5f;
        baby.setAdult(false);
        return baby;
    }

    @Override
    protected model.items.Carcass createCarcass() {
        return new model.items.Carcass(this.position.copy(), 25.0f, 100.0f, 120.0f, 100.0f, "Sói");
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        updateAnimation();
    }

    private void updateAnimation() {
        String animationState = getAnimationState();
        if ("attack".equals(animationState)) {
            this.imageVariant = "west.png"; // Yêu cầu: "Wolf: west.png vì chưa có frame attack"
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

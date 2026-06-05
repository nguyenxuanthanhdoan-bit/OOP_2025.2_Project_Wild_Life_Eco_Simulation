package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;
import model.strategies.HunterStrategy;
import model.strategies.FlockingStrategy;

public class Wolf extends CarnivoreAnimal {

    public Wolf(Vector2 position) {
        super(position, 28.0f, GameConfig.getInstance().WOLF_BASE_SPEED);

        this.speciesName = "Sói";
        this.maxHealth = 150.0f;
        this.health = this.maxHealth;
        this.maxHunger = 120.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 0.5f;
        this.maxThirst = 100.0f;
        this.visionRange = 450.0;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 0.6f;
        this.maxAge = 450.0f;

        // Sói có thể đi săn hoặc đi theo bầy (Hunter / Flocking)
        this.setStrategy(new HunterStrategy());
    }

    @Override
    public Animal reproduce() {
        return null;
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
        if ("attack".equals(this.actionState)) {
            this.imageVariant = "west.png"; // Yêu cầu: "Wolf: west.png vì chưa có frame attack"
        } else if ("run".equals(this.actionState)) {
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

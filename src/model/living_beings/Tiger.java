package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;
import model.strategies.HunterStrategy;

public class Tiger extends CarnivoreAnimal {

    public Tiger(Vector2 position) {
        super(position, 35.0f, GameConfig.getInstance().TIGER_BASE_SPEED);

        this.speciesName = "Hổ";
        this.maxHealth = 250.0f;
        this.health = this.maxHealth;
        this.maxHunger = 200.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 1.5f; 
        this.maxThirst = 150.0f;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 2.0f; 
        this.maxAge = 600.0f; 

        this.setStrategy(new HunterStrategy());
    }

    @Override
    public Animal reproduce() {
        return null;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        updateAnimation();
    }

    private void updateAnimation() {
        if ("attack".equals(this.actionState)) {
            this.imageVariant = "attack.png";
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

package model.items;

import core.DisplayMode;
import core.Vector2;

public class Meat extends FoodSource {
    private static final float DEFAULT_SIZE = 15.0f;
    private static final float DEFAULT_NUTRITION = 80.0f; // Thịt: 50 → 80
    private static final float MAX_DECAY_TIME = 90.0f;

    private float nutritionValue;
    private float remainingNutrition;
    private float decayTimer;

    public Meat(Vector2 position) {
        super(position, DEFAULT_SIZE);
        this.nutritionValue = DEFAULT_NUTRITION;
        this.remainingNutrition = DEFAULT_NUTRITION;
        this.decayTimer = MAX_DECAY_TIME;
        this.imageVariant = "Meat";
    }

    public float getNutritionValue() {
        return nutritionValue;
    }

    @Override
    public float consume(float amount) {
        if (!isAlive || remainingNutrition <= 0) return 0;

        float consumed = Math.min(amount, remainingNutrition);
        remainingNutrition -= consumed;

        if (remainingNutrition <= 0) {
            this.isAlive = false;
        } else {
            float ratio = remainingNutrition / nutritionValue;
            this.size = Math.max(DEFAULT_SIZE * 0.35f, DEFAULT_SIZE * (float) Math.sqrt(ratio));
        }

        return consumed;
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive) return;
        decayTimer -= deltaTime;
        if (decayTimer <= 0) {
            this.isAlive = false;
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ xử lý
    }
}

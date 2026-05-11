package plants;

import core.Vector2;
import entity.Entity;

public abstract class Plant extends Entity {
    protected float nutritionValue;

    public Plant(Vector2 position, float size, float nutritionValue) {
        super(position, size);
        this.nutritionValue = nutritionValue;
    }

    public float getNutritionValue() {
        return nutritionValue;
    }

    // Phase 1: Thực vật tạm thời chưa cần logic phức tạp
    @Override
    public void update(float deltaTime) {}
}
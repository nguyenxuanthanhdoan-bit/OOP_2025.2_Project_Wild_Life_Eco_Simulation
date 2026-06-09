package model.structures;

import core.GameConfig;
import core.Vector2;
import model.entity.Structure;

/**
 * Kho thức ăn của làng.
 * Hiện tại lưu lượng thức ăn tổng quát để chuẩn bị cho Villager/Hunter.
 */
public class FoodStorage extends Structure {
    private static final String IMAGE_VARIANT = "food_storage";

    private final float capacity;
    private float storedFood;

    public FoodStorage(Vector2 position) {
        this(position, GameConfig.getInstance().FOOD_STORAGE_CAPACITY);
    }

    public FoodStorage(Vector2 position, float capacity) {
        super(position, GameConfig.getInstance().FOOD_STORAGE_SIZE, "FOOD_STORAGE", IMAGE_VARIANT, true);
        this.capacity = Math.max(1.0f, capacity);
        this.storedFood = 0.0f;
    }

    public float addFood(float amount) {
        if (amount <= 0) return 0.0f;
        float accepted = Math.min(amount, capacity - storedFood);
        storedFood += accepted;
        return accepted;
    }

    public float consumeFood(float amount) {
        if (amount <= 0) return 0.0f;
        float consumed = Math.min(amount, storedFood);
        storedFood -= consumed;
        return consumed;
    }

    public boolean hasFood() {
        return storedFood > 0.0f;
    }

    public float getStoredFood() {
        return storedFood;
    }

    public float getCapacity() {
        return capacity;
    }
}

package model.plants;

import core.Vector2;
import core.StructureType;
import model.entity.Entity;
import model.entity.Structure; // Bổ sung import này

/**
 * Lớp trừu tượng trung gian dành riêng cho các loài thực vật.
 * Kế thừa từ Structure để được hưởng các đặc tính của vật tĩnh (isWalkable, StructureType).
 */
public abstract class Plant extends Structure {

    protected float nutritionValue;

    // Bổ sung StructureType vào tham số truyền vào
    public Plant(Vector2 position, float size, StructureType type, boolean isWalkable, float nutritionValue) {
        // Truyền biến isWalkable xuống cho ông nội Structure thay vì ép cứng là true
        super(position, size, type, isWalkable, false);
        this.nutritionValue = nutritionValue;
    }

    public float getNutritionValue() {
        return nutritionValue;
    }

    // Vì Plant là Abstract, ta có thể để dành hàm onInteract() cho các class con (Grass, FruitTree) tự quyết định logic bị ăn.
}
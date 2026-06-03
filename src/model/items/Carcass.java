package model.items;

import core.Vector2;
import core.DisplayMode;

/**
 * Lớp trừu tượng quản lý xác động vật.
 */
public class Carcass extends FoodSource {
    protected float nutritionValue;
    protected float decayTimer;
    protected float currentMass;
    protected float initialMass;
    protected String sourceSpecies;

    public Carcass(Vector2 position, float size, float nutritionValue, float decayTime, float initialMass, String sourceSpecies) {
        super(position, size);
        this.nutritionValue = nutritionValue;
        this.decayTimer = decayTime;
        this.currentMass = initialMass;
        this.initialMass = initialMass;
        this.sourceSpecies = sourceSpecies;
        this.imageVariant = "Meat"; // Fallback dùng hình ảnh cục thịt có sẵn
    }

    public String getSourceSpecies() {
        return sourceSpecies;
    }
    
    public float getCurrentMass() {
        return currentMass;
    }

    @Override
    public float consume(float amount) {
        if (!isAlive || currentMass <= 0) return 0;
        
        float consumed = Math.min(amount, currentMass);
        currentMass -= consumed;
        
        if (currentMass <= 0) {
            this.isAlive = false; // Bị ăn hết, xóa khỏi thế giới
        }
        
        // Tỷ lệ dinh dưỡng nhận được tương ứng với khối lượng ăn được
        return consumed * (nutritionValue / initialMass);
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive) return;
        decayTimer -= deltaTime;
        if (decayTimer <= 0) {
            this.isAlive = false; // Phân hủy tự nhiên
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng imageVariant để vẽ
    }
}

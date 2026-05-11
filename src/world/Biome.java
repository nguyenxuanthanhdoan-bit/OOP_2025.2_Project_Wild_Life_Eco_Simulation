package world;

import core.Vector2;
import core.BiomeType;
import core.DisplayMode;
import entity.Entity;

/**
 * Lớp cơ sở cho các loại địa hình trong thế giới.
 */
public abstract class Biome extends Entity {

    protected BiomeType biomeType;
    protected float speedModifier; // Hệ số ảnh hưởng tốc độ di chuyển

    public Biome(Vector2 position, float size, BiomeType type, float speedModifier) {
        // Biome cũng là một Entity, size ở đây có thể hiểu là kích thước vùng địa hình
        super(position, size);
        this.biomeType = type;
        this.speedModifier = speedModifier;
    }

    public BiomeType getBiomeType() {
        return biomeType;
    }

    public float getSpeedModifier() {
        return speedModifier;
    }

    /**
     * Kiểm tra xem một vị trí có nằm trong vùng của Biome này không.
     */
    public boolean containsPoint(Vector2 pos) {
        // Logic đơn giản cho Phase 1: Coi Biome là một hình vuông
        float halfSize = this.size / 2.0f;
        return pos.x >= position.x - halfSize && pos.x <= position.x + halfSize &&
                pos.y >= position.y - halfSize && pos.y <= position.y + halfSize;
    }

    @Override
    public void update(float deltaTime) {
        // Địa hình thường tĩnh, không cần update logic trong Phase 1
    }
}
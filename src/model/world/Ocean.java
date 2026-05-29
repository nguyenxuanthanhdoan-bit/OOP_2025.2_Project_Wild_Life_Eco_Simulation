package model.world;

import core.Vector2;
import core.BiomeType;
import core.DisplayMode;

public class Ocean extends Biome {
    public Ocean(Vector2 position, float size) {
        super(position, size, BiomeType.OCEAN, 1.0f);
    }

    @Override
    public void render(DisplayMode mode) {
        // Nước đã được vẽ bằng TMX layer
    }
}

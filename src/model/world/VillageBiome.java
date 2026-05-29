package model.world;

import core.Vector2;
import core.BiomeType;
import core.DisplayMode;

public class VillageBiome extends Biome {
    public VillageBiome(Vector2 position, float size) {
        super(position, size, BiomeType.VILLAGE, 1.0f);
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

package model.living_beings;

import model.world.WaterTile;
import model.world.WaterBiome;

public class OceanOnlyRule implements HabitatRule {
    @Override
    public boolean canLive(WaterTile tile) {
        if (tile == null) return false;
        return tile.getBiome() == WaterBiome.OCEAN;
    }
}

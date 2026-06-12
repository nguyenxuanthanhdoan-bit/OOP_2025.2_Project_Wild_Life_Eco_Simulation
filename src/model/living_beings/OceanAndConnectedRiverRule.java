package model.living_beings;

import model.living_beings.animal.Animal;

import model.world.WaterTile;
import model.world.WaterBiome;

public class OceanAndConnectedRiverRule implements HabitatRule {
    @Override
    public boolean canLive(WaterTile tile) {
        if (tile == null) return false;
        return tile.getBiome() == WaterBiome.OCEAN || 
              (tile.getBiome() == WaterBiome.RIVER && tile.isConnectedToOcean());
    }
}

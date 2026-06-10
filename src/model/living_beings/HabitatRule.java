package model.living_beings;

import model.world.WaterTile;

public interface HabitatRule {
    boolean canLive(WaterTile tile);
}

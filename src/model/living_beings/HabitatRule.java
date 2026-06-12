package model.living_beings;

import model.living_beings.animal.Animal;

import model.world.WaterTile;

public interface HabitatRule {
    boolean canLive(WaterTile tile);
}

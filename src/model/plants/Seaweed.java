package model.plants;

import core.Vector2;
import core.DisplayMode;

public class Seaweed extends Plant {
    private static final String[] VARIANTS = {
        "seaweed_1", "seaweed_2", "seaweed_3", 
        "seaweed_4", "seaweed_5"
    };

    public Seaweed(Vector2 position) {
        super(position, 25.0f, 10.0f);
        this.isSolid = false;
        this.imageVariant = VARIANTS[new java.util.Random().nextInt(VARIANTS.length)];
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

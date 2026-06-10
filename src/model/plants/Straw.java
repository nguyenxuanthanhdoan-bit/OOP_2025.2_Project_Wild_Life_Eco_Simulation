package model.plants;

import core.Vector2;
import core.DisplayMode;

public class Straw extends Plant {
    private static final String[] VARIANTS = { "straw_1", "straw_2" };

    public Straw(Vector2 position) {
        super(position, 30.0f, 0.0f);
        this.isSolid = true;
        this.imageVariant = VARIANTS[new java.util.Random().nextInt(VARIANTS.length)];
        this.collider = new model.collision.Collider(this, 10.0f, model.collision.CollisionLayer.OBSTACLE);
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

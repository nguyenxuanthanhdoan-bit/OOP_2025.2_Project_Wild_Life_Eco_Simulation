package model.entity;

import core.DisplayMode;
import core.Vector2;
import model.collision.Collider;
import model.collision.CollisionLayer;

/**
 * Base class cho công trình và vật trang trí tĩnh trong world.
 */
public abstract class Structure extends Entity {
    private final String structureType;

    protected Structure(Vector2 position, float size, String structureType, String imageVariant, boolean solid) {
        super(position, size);
        this.structureType = structureType;
        this.imageVariant = imageVariant == null ? "" : imageVariant;
        this.isSolid = solid;
        if (solid) {
            this.collider = new Collider(this, size * 0.38f, CollisionLayer.OBSTACLE);
        }
    }

    public String getStructureType() {
        return structureType;
    }

    @Override
    public void update(float deltaTime) {
        // Static structure.
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem draws structures from imageVariant.
    }
}

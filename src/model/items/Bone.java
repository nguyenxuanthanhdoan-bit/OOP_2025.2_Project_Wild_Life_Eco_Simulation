package model.items;

import core.DisplayMode;
import core.Vector2;
import model.entity.Entity;

public class Bone extends Entity {
    private static final float DEFAULT_SIZE = 15.0f;
    private static final float MAX_DECAY_TIME = 120.0f;

    private float decayTimer;

    public Bone(Vector2 position) {
        super(position, DEFAULT_SIZE);
        this.decayTimer = MAX_DECAY_TIME;
        this.isSolid = false;
        this.imageVariant = "Bone";
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive) return;
        decayTimer -= deltaTime;
        if (decayTimer <= 0) {
            this.isAlive = false;
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ xử lý
    }
}

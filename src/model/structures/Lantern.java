package model.structures;

import core.Vector2;
import model.entity.Entity;
import core.DisplayMode;

public class Lantern extends Entity {
    private String lanternType;
    private float animationTime = 0;
    
    public Lantern(Vector2 position, String lanternType) {
        super(position, 16);
        this.lanternType = lanternType;
        this.isSolid = true; 
    }

    public String getLanternType() {
        return lanternType;
    }

    @Override
    public void update(float deltaTime) {
        animationTime += deltaTime;
    }

    @Override
    public void render(DisplayMode mode) {
        // Render logic is handled in RenderSystem
    }

    public float getAnimationTime() {
        return animationTime;
    }
}

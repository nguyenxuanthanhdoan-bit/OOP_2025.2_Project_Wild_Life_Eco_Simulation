package model.plants;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;
import model.living_beings.Animal;

public class Cactus extends Plant {
    private float damageRate = 5.0f; // 5 HP/s

    public Cactus(Vector2 position) {
        super(position, 30.0f, 0.0f);
        this.isSolid = true;
        this.imageVariant = "cactus";
        this.collider = new model.collision.Collider(this, 15.0f, model.collision.CollisionLayer.OBSTACLE);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (world != null && world.getSpatialGrid() != null) {
            java.util.List<Entity> neighbors = world.getSpatialGrid().getNeighbors(this.position, this.size * 0.5f + 20.0f);
            for (Entity e : neighbors) {
                if (e instanceof Animal && e.isAlive()) {
                    float dist = this.position.distanceTo(e.getPosition());
                    float combinedRadius = (this.collider != null ? this.collider.getRadius() : this.size / 2) +
                                           (e.getCollider() != null ? e.getCollider().getRadius() : e.getSize() / 2);
                    if (dist < combinedRadius + 5.0f) {
                        ((Animal) e).takeDamage(damageRate * deltaTime);
                    }
                }
            }
        }
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

package model.collision;

import model.entity.Entity;

public class Collider {
    private float radius;
    private CollisionLayer layer;
    private Entity owner;

    public Collider(Entity owner, float radius, CollisionLayer layer) {
        this.owner = owner;
        this.radius = radius;
        this.layer = layer;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public CollisionLayer getLayer() {
        return layer;
    }

    public Entity getOwner() {
        return owner;
    }
}

package model.items;

import core.DisplayMode;
import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;

public class FireballProjectile extends Entity {
    private final Animal target;
    private final float speed;
    private final float damage;
    private float lifeTime = 2.5f;
    private final Vector2 direction = new Vector2(1.0f, 0.0f);

    public FireballProjectile(Vector2 position, Animal target, float speed, float damage, float size) {
        super(position, size);
        this.target = target;
        this.speed = speed;
        this.damage = damage;
        this.imageVariant = "dart";
        this.isSolid = false;
    }

    @Override
    public void update(float deltaTime) {
        if (target == null || !target.isAlive()) {
            setAlive(false);
            return;
        }

        lifeTime -= deltaTime;
        if (lifeTime <= 0.0f) {
            setAlive(false);
            return;
        }

        Vector2 toTarget = target.getPosition().copy().subtract(position);
        float dist = toTarget.length();
        float hitRange = size / 2.0f + target.getSize() / 2.0f;
        if (dist <= hitRange) {
            target.takeDamage(damage);
            setAlive(false);
            return;
        }

        if (dist > Vector2.EPSILON) {
            toTarget.normalize();
            direction.set(toTarget);
            position.add(toTarget.scale(speed * deltaTime));
        }
    }

    public float getRotationRadians() {
        return (float) Math.atan2(direction.y, direction.x);
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem vẽ projectile này trực tiếp.
    }
}

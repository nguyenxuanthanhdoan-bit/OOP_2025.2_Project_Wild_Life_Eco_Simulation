package model.structures;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.entity.Structure;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Nhà trong village.
 * Có sức chứa nhiều entity để sau này dân làng có thể trốn/nghỉ bên trong.
 */
public class House extends Structure {
    private final int capacity;
    private final Set<Entity> occupants = new LinkedHashSet<>();

    public House(Vector2 position, int variant) {
        this(position, variant, GameConfig.getInstance().HOUSE_CAPACITY);
    }

    public House(Vector2 position, int variant, int capacity) {
        super(position, GameConfig.getInstance().HOUSE_SIZE, "HOUSE", "house_" + normalizeVariant(variant), true);
        this.capacity = Math.max(1, capacity);
        // Tăng bán kính va chạm để động vật không đi xuyên nhà
        this.setCollider(new model.collision.Collider(this, GameConfig.getInstance().HOUSE_SIZE * 0.6f, model.collision.CollisionLayer.OBSTACLE));
    }

    public boolean enter(Entity entity) {
        if (entity == null) return false;
        if (occupants.contains(entity)) return true;
        if (occupants.size() >= capacity) return false;
        return occupants.add(entity);
    }

    public void exit(Entity entity) {
        occupants.remove(entity);
    }

    public boolean contains(Vector2 pos) {
        return pos != null && this.position.distanceTo(pos) <= this.size * 0.55f;
    }

    public boolean hasSpace() {
        return occupants.size() < capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getOccupantCount() {
        return occupants.size();
    }

    public Set<Entity> getOccupants() {
        return Collections.unmodifiableSet(occupants);
    }

    private static int normalizeVariant(int variant) {
        return Math.max(1, variant);
    }
}

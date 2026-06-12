package model.living_beings;

import core.Vector2;
import model.entity.Entity;
import model.plants.Plant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class AnimalMemory {
    public static class DangerZone {
        public Vector2 position;
        public float radius;
        public float timer;
        public DangerZone(Vector2 position, float radius, float timer) {
            this.position = position;
            this.radius = radius;
            this.timer = timer;
        }
    }

    private final Map<UUID, DangerZone> dangerZones = new HashMap<>();
    private final Map<UUID, Float> unsafeFoodMemory = new HashMap<>();

    public void markDangerZone(Entity predator, float radius, float duration) {
        dangerZones.put(predator.getId(), new DangerZone(predator.getPosition().copy(), radius, duration));
    }

    public boolean isInDangerZone(Vector2 pos) {
        if (dangerZones.isEmpty()) return false;
        for (DangerZone dz : dangerZones.values()) {
            if (pos.distanceTo(dz.position) <= dz.radius) {
                return true;
            }
        }
        return false;
    }

    public void updateDangerZones(float deltaTime) {
        if (dangerZones.isEmpty()) return;
        Iterator<Map.Entry<UUID, DangerZone>> it = dangerZones.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DangerZone> entry = it.next();
            entry.getValue().timer -= deltaTime;
            if (entry.getValue().timer <= 0) {
                it.remove();
            }
        }
    }

    public void rememberUnsafeFood(Entity food, float duration) {
        if (food == null) return;
        unsafeFoodMemory.put(food.getId(), Math.max(duration, unsafeFoodMemory.getOrDefault(food.getId(), 0.0f)));
    }

    public boolean isFoodUnsafe(Entity food) {
        if (food == null) return false;
        Float timer = unsafeFoodMemory.get(food.getId());
        return timer != null && timer > 0;
    }

    public void updateUnsafeFoodMemory(float deltaTime) {
        if (unsafeFoodMemory.isEmpty()) return;
        Iterator<Map.Entry<UUID, Float>> iterator = unsafeFoodMemory.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Float> entry = iterator.next();
            float newTime = entry.getValue() - deltaTime;
            if (newTime <= 0) {
                iterator.remove();
            } else {
                entry.setValue(newTime);
            }
        }
    }
}

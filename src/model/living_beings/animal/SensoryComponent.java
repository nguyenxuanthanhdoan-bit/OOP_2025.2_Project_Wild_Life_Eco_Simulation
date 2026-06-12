package model.living_beings.animal;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Human;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SensoryComponent {

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

    private final Animal owner;
    
    // Variables
    private double visionRange;
    private final Map<UUID, Float> unsafeFoodMemory = new HashMap<>();
    private final Map<UUID, DangerZone> dangerZones = new HashMap<>();

    private float radarCooldown = 0f;
    private boolean cachedThreat = false;
    private float gardenThreatCooldown = 0f;
    private boolean cachedGardenThreat = false;

    public SensoryComponent(Animal owner, double visionRange) {
        this.owner = owner;
        this.visionRange = visionRange;
    }

    public void update(float deltaTime) {
        updateDangerZones(deltaTime);
        updateUnsafeFoodMemory(deltaTime);
        if (radarCooldown > 0) {
            radarCooldown -= deltaTime;
        }
        if (gardenThreatCooldown > 0) {
            gardenThreatCooldown -= deltaTime;
        }
    }

    public double getVisionRange() {
        return visionRange;
    }

    public void setVisionRange(double visionRange) {
        this.visionRange = visionRange;
    }

    public boolean detectDangerousThreats() {
        if (!owner.getProfile().canBeScared()) return false;
        
        if (radarCooldown > 0) {
            return cachedThreat;
        }
        radarCooldown = 0.5f; // Quét 0.5s 1 lần

        model.world.World worldRef = owner.getWorldRef();
        if (worldRef == null || worldRef.getSpatialGrid() == null) return false;

        java.util.List<Entity> neighbors =
            worldRef.getSpatialGrid().getNeighbors(owner.getPosition(), (float) this.visionRange);

        for (Entity e : neighbors) {
            if (!(e instanceof Animal) || e == owner || !e.isAlive()) continue;
            Animal other = (Animal) e;
            if (owner.isThreatenedBy(other)) {
                model.strategies.IStrategy otherStrategy = other.getCurrentStrategy();
                if (otherStrategy instanceof model.strategies.SleepStrategy) {
                    continue; // Đang ngủ -> Bỏ qua
                }
                boolean isHunting = otherStrategy instanceof model.strategies.HunterStrategy;
                float distSq = owner.getPosition().distanceSquared(other.getPosition());
                float maxDist = owner instanceof Human
                        ? core.GameConfig.getInstance().THREAT_RADIUS
                        : isHunting ? (float) this.visionRange : (float) this.visionRange * 0.5f;
                
                if (distSq <= maxDist * maxDist) {
                    cachedThreat = true;
                    return true;
                }
            }
        }
        cachedThreat = false;
        return false;
    }

    public boolean hasDangerousThreats() {
        return detectDangerousThreats();
    }

    public boolean hasGardenThreat() {
        if (!owner.getProfile().avoidsGuardedGardens()) return false;
        if (gardenThreatCooldown > 0) {
            return cachedGardenThreat;
        }
        gardenThreatCooldown = 0.5f;
        cachedGardenThreat = false;

        model.world.World worldRef = owner.getWorldRef();
        if (worldRef == null) return false;
        cachedGardenThreat = worldRef.getCropManager()
                .isGuardedGardenNear(worldRef, owner.getPosition(), (float) visionRange);
        return cachedGardenThreat;
    }

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

    private void updateDangerZones(float deltaTime) {
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

    public void markFoodUnsafe(Entity food, float duration) {
        if (food == null || duration <= 0) return;
        unsafeFoodMemory.put(food.getId(), Math.max(duration, unsafeFoodMemory.getOrDefault(food.getId(), 0.0f)));
    }

    public boolean isFoodMarkedUnsafe(Entity food) {
        if (food == null) return false;
        Float timer = unsafeFoodMemory.get(food.getId());
        return timer != null && timer > 0;
    }

    private void updateUnsafeFoodMemory(float deltaTime) {
        if (unsafeFoodMemory.isEmpty()) return;
        Iterator<Map.Entry<UUID, Float>> iterator = unsafeFoodMemory.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Float> entry = iterator.next();
            float remaining = entry.getValue() - deltaTime;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }
}

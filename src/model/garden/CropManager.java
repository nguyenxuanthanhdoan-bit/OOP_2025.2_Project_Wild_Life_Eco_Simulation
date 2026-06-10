package model.garden;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Human;
import model.structures.GardenBed;
import model.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quản lý toàn bộ vườn (các GardenBed) trong thế giới.
 */
public class CropManager {

    private final List<GardenBed> gardens = new ArrayList<>();
    private final Map<UUID, Boolean> guardedCache = new HashMap<>();
    private final Map<UUID, Float> guardedCacheTtl = new HashMap<>();
    private static final float GUARD_CACHE_SECONDS = 0.5f;

    public void addGardenBed(GardenBed bed) {
        if (!gardens.contains(bed)) {
            gardens.add(bed);
        }
    }

    public void clear() {
        gardens.clear();
        guardedCache.clear();
        guardedCacheTtl.clear();
    }

    public List<GardenBed> getGardens() {
        return gardens;
    }

    /**
     * Cập nhật thời gian sinh trưởng của toàn bộ cây trồng.
     */
    public void update(float deltaTime, World world) {
        for (GardenBed bed : gardens) {
            bed.updateCrop(deltaTime, world);
        }
        guardedCacheTtl.replaceAll((id, ttl) -> Math.max(0.0f, ttl - deltaTime));
    }

    /**
     * Tìm chậu cây đã chín gần nhất cho AI thu hoạch.
     */
    public GardenBed findNearestMatureCrop(Vector2 pos) {
        if (pos == null) return null;

        GardenBed nearest = null;
        float minDist = Float.MAX_VALUE;

        for (GardenBed bed : gardens) {
            if (bed.isMature() && !bed.isBeingHarvested()) {
                float dist = pos.distanceTo(bed.getPosition());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = bed;
                }
            }
        }

        return nearest;
    }

    public GardenBed reserveNearestMatureCrop(Human human) {
        if (human == null) return null;

        GardenBed nearest = null;
        float minDist = Float.MAX_VALUE;
        Iterable<GardenBed> candidates = human.getHomeSettlement() != null
                ? human.getHomeSettlement().getGardenBeds()
                : gardens;

        for (GardenBed bed : candidates) {
            if (!bed.isMature() || bed.isBeingHarvested()) continue;
            float dist = human.getPosition().distanceTo(bed.getPosition());
            if (dist < minDist) {
                minDist = dist;
                nearest = bed;
            }
        }
        return nearest != null && nearest.reserve(human) ? nearest : null;
    }

    /**
     * Kiểm tra xem một vị trí có nằm trong vùng của vườn (chậu cây) nào không,
     * dùng để block các loài thú dữ dẫm lên cây trồng.
     */
    public boolean isInsideGarden(Vector2 pos) {
        if (pos == null) return false;
        for (GardenBed bed : gardens) {
            // Tăng bán kính chặn động vật lên gấp nhiều lần để chúng cách xa vườn
            if (bed.getPosition().distanceTo(pos) < bed.getSize() * 3.0f) {
                return true;
            }
        }
        return false;
    }

    public boolean isGuardedGardenNear(World world, Vector2 position, float searchRadius) {
        if (world == null || position == null || world.getSpatialGrid() == null) return false;

        for (GardenBed bed : gardens) {
            if (!bed.isAlive()
                    || position.distanceTo(bed.getPosition()) > searchRadius + 250.0f) {
                continue;
            }
            if (isGardenGuarded(world, bed)) return true;
        }
        return false;
    }

    private boolean isGardenGuarded(World world, GardenBed bed) {
        UUID id = bed.getId();
        if (guardedCacheTtl.getOrDefault(id, 0.0f) > 0.0f) {
            return guardedCache.getOrDefault(id, false);
        }

        boolean guarded = false;
        for (Entity entity : world.getSpatialGrid().getNeighbors(bed.getPosition(), 250.0f)) {
            if (entity instanceof Human && entity.isAlive()) {
                guarded = true;
                break;
            }
        }
        guardedCache.put(id, guarded);
        guardedCacheTtl.put(id, GUARD_CACHE_SECONDS);
        return guarded;
    }
}

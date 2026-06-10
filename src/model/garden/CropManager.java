package model.garden;

import core.Vector2;
import model.structures.GardenBed;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý toàn bộ vườn (các GardenBed) trong thế giới.
 */
public class CropManager {

    private final List<GardenBed> gardens = new ArrayList<>();

    public void addGardenBed(GardenBed bed) {
        if (!gardens.contains(bed)) {
            gardens.add(bed);
        }
    }

    public void clear() {
        gardens.clear();
    }

    public List<GardenBed> getGardens() {
        return gardens;
    }

    /**
     * Cập nhật thời gian sinh trưởng của toàn bộ cây trồng.
     */
    public void update(float deltaTime) {
        for (GardenBed bed : gardens) {
            bed.updateCrop(deltaTime);
        }
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
}

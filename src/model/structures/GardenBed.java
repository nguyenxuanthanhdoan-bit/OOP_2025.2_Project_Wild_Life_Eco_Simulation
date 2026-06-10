package model.structures;

import core.GameConfig;
import core.Vector2;
import model.entity.Structure;
import model.garden.CropState;
import model.garden.CropType;

import java.util.UUID;

/**
 * Một chậu cây trong vườn.
 * Có chức năng quản lý logic sinh trưởng của chính nó.
 */
public class GardenBed extends Structure {

    private CropType currentCropType;
    private CropState currentState;
    private float growthTimer;
    
    private UUID reservedBy;

    public GardenBed(Vector2 position) {
        // Khởi tạo là một Structure với size = GARDEN_BED_SIZE, solid = false (để AI có thể đứng sát hoặc đi vào nếu cần)
        // Chúng ta sẽ đặt solid=false để không dùng va chạm mặc định, mà dùng GameMap/World custom collision rule
        super(position, GameConfig.getInstance().GARDEN_BED_SIZE, "GARDEN_BED", "garden_seed", false);
        
        this.currentCropType = CropType.BASIC_FLOWER;
        this.currentState = CropState.SEED;
        this.growthTimer = 0f;
    }

    public void updateCrop(float deltaTime, model.world.World world) {
        if (currentState == CropState.MATURE) return; // Đã lớn, chờ thu hoạch

        float currentDelta = deltaTime;
        if (world != null && world.getCurrentSeason() == model.world.World.Season.WINTER) {
            currentDelta *= (1.0f - world.getWinterProgress() * 0.8f);
        }

        growthTimer += currentDelta;
        if (growthTimer >= currentCropType.getStateDuration()) {
            growthTimer = 0f;
            advanceState();
        }
    }

    private void advanceState() {
        if (currentState == CropState.SEED) {
            currentState = CropState.GROWING;
            this.imageVariant = currentCropType.getSpriteForState(CropState.GROWING);
        } else if (currentState == CropState.GROWING) {
            currentState = CropState.MATURE;
            this.imageVariant = currentCropType.getSpriteForState(CropState.MATURE);
        }
    }

    public float harvest(model.entity.Entity owner) {
        if (!isReservedBy(owner) || currentState != CropState.MATURE) return 0.0f;
        float yield = currentCropType.getFoodYield();
        this.reservedBy = null;
        this.currentState = CropState.SEED;
        this.growthTimer = 0f;
        this.imageVariant = currentCropType.getSpriteForState(CropState.SEED);
        return yield;
    }

    public CropState getCurrentState() {
        return currentState;
    }

    public boolean isMature() {
        return currentState == CropState.MATURE;
    }

    public boolean isBeingHarvested() {
        return reservedBy != null;
    }

    public boolean reserve(model.entity.Entity owner) {
        if (owner == null || !isMature()) return false;
        if (reservedBy == null || reservedBy.equals(owner.getId())) {
            reservedBy = owner.getId();
            return true;
        }
        return false;
    }

    public void releaseReservation(model.entity.Entity owner) {
        if (isReservedBy(owner)) reservedBy = null;
    }

    public boolean isReservedBy(model.entity.Entity owner) {
        return owner != null && reservedBy != null && reservedBy.equals(owner.getId());
    }
}

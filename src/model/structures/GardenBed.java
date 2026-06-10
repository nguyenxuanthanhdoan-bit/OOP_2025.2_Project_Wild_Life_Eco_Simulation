package model.structures;

import core.GameConfig;
import core.Vector2;
import model.entity.Structure;
import model.garden.CropState;
import model.garden.CropType;

/**
 * Một chậu cây trong vườn.
 * Có chức năng quản lý logic sinh trưởng của chính nó.
 */
public class GardenBed extends Structure {

    private CropType currentCropType;
    private CropState currentState;
    private float growthTimer;
    
    // Đánh dấu để AI biết chậu này đang được ai đó thu hoạch
    private boolean beingHarvested = false;

    public GardenBed(Vector2 position) {
        // Khởi tạo là một Structure với size = GARDEN_BED_SIZE, solid = false (để AI có thể đứng sát hoặc đi vào nếu cần)
        // Chúng ta sẽ đặt solid=false để không dùng va chạm mặc định, mà dùng GameMap/World custom collision rule
        super(position, GameConfig.getInstance().GARDEN_BED_SIZE, "GARDEN_BED", "garden_seed", false);
        
        this.currentCropType = CropType.BASIC_FLOWER;
        this.currentState = CropState.SEED;
        this.growthTimer = 0f;
    }

    public void updateCrop(float deltaTime) {
        if (currentState == CropState.MATURE) return; // Đã lớn, chờ thu hoạch

        growthTimer += deltaTime;
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

    public void harvest() {
        this.beingHarvested = false;
        this.currentState = CropState.SEED;
        this.growthTimer = 0f;
        this.imageVariant = currentCropType.getSpriteForState(CropState.SEED);
    }

    public CropState getCurrentState() {
        return currentState;
    }

    public boolean isMature() {
        return currentState == CropState.MATURE;
    }

    public boolean isBeingHarvested() {
        return beingHarvested;
    }

    public void setBeingHarvested(boolean beingHarvested) {
        this.beingHarvested = beingHarvested;
    }
}

package model.garden;

/**
 * Định nghĩa cấu hình cho các loại cây trồng.
 * Thiết kế theo hướng Data-driven, có thể dễ dàng thêm mới các loại cây trồng khác.
 */
public class CropType {

    // Danh sách các loại cây trồng có sẵn
    public static final CropType BASIC_FLOWER = new CropType(
            "Basic Flower",
            20.0f, // Mất 20s để chuyển từ SEED -> GROWING, và 20s từ GROWING -> MATURE
            new String[]{"garden_seed", "garden_growing", "garden_mature"}
    );

    // Bạn có thể thêm các cây khác vào đây, ví dụ:
    // public static final CropType CARROT = new CropType("Carrot", 20.0f, new String[]{"carrot_seed", "carrot_grow", "carrot_mature"});

    private final String name;
    private final float stateDuration;
    private final String[] stateSprites;

    public CropType(String name, float stateDuration, String[] stateSprites) {
        this.name = name;
        this.stateDuration = stateDuration;
        this.stateSprites = stateSprites;
    }

    public String getName() {
        return name;
    }

    public float getStateDuration() {
        return stateDuration;
    }

    public String getSpriteForState(CropState state) {
        if (stateSprites == null || stateSprites.length == 0) return null;
        switch (state) {
            case SEED:
                return stateSprites[0];
            case GROWING:
                return stateSprites.length > 1 ? stateSprites[1] : stateSprites[0];
            case MATURE:
                return stateSprites.length > 2 ? stateSprites[2] : stateSprites[0];
            default:
                return stateSprites[0];
        }
    }
}

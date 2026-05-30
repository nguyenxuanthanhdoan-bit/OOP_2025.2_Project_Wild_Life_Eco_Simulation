package model.structures;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;
import java.util.Random;

public class Rock extends Entity {
    
    public Rock(Vector2 position) {
        // Tảng đá có kích thước trung bình 30
        super(position, 60.0f);
        this.isSolid = true; // Là vật cản
        
        // Random hình ảnh từ Rock_1 đến Rock_3
        Random random = new Random();
        int variant = random.nextInt(3) + 1;
        this.imageVariant = "Rock_" + variant;
    }

    @Override
    public void update(float deltaTime) {
        // Tảng đá đứng yên, không có logic cập nhật
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Rock_1", "Rock_2", "Rock_3") để vẽ hình
    }
}

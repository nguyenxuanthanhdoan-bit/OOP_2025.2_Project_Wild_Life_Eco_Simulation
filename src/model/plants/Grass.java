package model.plants;

import core.Vector2;
import core.DisplayMode;
import java.util.Random;

public class Grass extends Plant {
    public Grass(Vector2 position) {
        super(position, 15.0f, 10.0f);
        this.isSolid = false; // Cỏ có thể dẫm lên (không cản đường)
        
        // Random hình ảnh từ Grass_1 đến Grass_2
        Random random = new Random();
        int variant = random.nextInt(2) + 1;
        this.imageVariant = "Grass_" + variant;
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Grass_1" hoặc "Grass_2") để vẽ hình
    }
}
package model.plants;

import core.Vector2;
import core.DisplayMode;
import java.util.Random;

public class FruitTree extends Plant {

    public FruitTree(Vector2 position) {
        // Cây không bị ăn nên dinh dưỡng = 0, kích thước mặc định 60.0f
        super(position, 60.0f, 0);
        this.isSolid = true; // Là vật cản
        
        // Random hình ảnh từ Tree_1 đến Tree_5
        Random random = new Random();
        int variant = random.nextInt(5) + 1;
        this.imageVariant = "Tree_" + variant;
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Tree_1", ..., "Tree_5") để vẽ hình
    }
}
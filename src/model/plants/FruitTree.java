package model.plants;

import core.Vector2;
import core.DisplayMode;
import java.util.Random;

public class FruitTree extends Plant {

    public FruitTree(Vector2 position) {
        this(position, new Random().nextInt(13) + 1);
    }

    public FruitTree(Vector2 position, int variantId) {
        // Cây không bị ăn nên dinh dưỡng = 0, kích thước mặc định 60.0f
        super(position, 60.0f, 0);
        this.isSolid = true; // Là vật cản
        this.imageVariant = "Tree_" + variantId;
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Tree_1", ..., "Tree_5") để vẽ hình
    }
}
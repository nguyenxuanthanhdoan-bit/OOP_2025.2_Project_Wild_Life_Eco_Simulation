package model.plants;

import core.Vector2;
import core.DisplayMode;

public class FruitTree extends Plant {
    private boolean isSmall; // Để phân biệt cây to/nhỏ từ assets của bạn

    public FruitTree(Vector2 position, boolean isSmall) {
        // Cây không bị ăn nên dinh dưỡng = 0, size tùy thuộc vào loại cây
        super(position, isSmall ? 60.0f : 60.0f, 0);
        this.isSmall = isSmall;
    }

    public boolean isSmall() {
        return isSmall;
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ chọn "Oak_Tree.png" hoặc "Oak_Tree_Small.png"
    }
}
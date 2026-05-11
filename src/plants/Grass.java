package plants;

import core.Vector2;
import core.DisplayMode;

public class Grass extends Plant {
    public Grass(Vector2 position) {
        // TĂNG KÍCH THƯỚC: Thay 10.0f bằng 30.0f
        super(position, 15.0f, 10.0f);
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ đảm nhận việc vẽ
    }
}
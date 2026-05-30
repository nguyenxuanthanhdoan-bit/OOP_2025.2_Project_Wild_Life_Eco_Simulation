package model.structures;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;
import java.util.Random;

public class Bush extends Entity {
    
    public Bush(Vector2 position) {
        // Bụi rậm có kích thước khoảng 25.0f
        super(position, 25.0f);
        this.isSolid = false; // Bụi rậm là thực thể mềm, thú có thể đi xuyên qua
        
        // Random hình ảnh từ Bush_1 đến Bush_2
        Random random = new Random();
        int variant = random.nextInt(2) + 1;
        this.imageVariant = "Bush_" + variant;
    }

    @Override
    public void update(float deltaTime) {
        // Bụi rậm đứng yên
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Bush_1" hoặc "Bush_2") để vẽ hình
    }
}

package living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;
import strategies.PassiveStrategy;

public class Rabbit extends HerbivoreAnimal {

    public Rabbit(Vector2 position) {
        // Khởi tạo thỏ với kích thước 20.0f và tốc độ lấy từ GameConfig
        super(position, 20.0f, GameConfig.getInstance().RABBIT_BASE_SPEED);

        // Gắn "bộ não" mặc định cho Thỏ ở Phase 1 là đi dạo/đứng im (Passive)
        this.setStrategy(new PassiveStrategy());
    }

    @Override
    public void render(DisplayMode mode) {
        // Hàm này để trống vì RenderSystem sẽ đảm nhận việc vẽ hình ảnh
    }
}
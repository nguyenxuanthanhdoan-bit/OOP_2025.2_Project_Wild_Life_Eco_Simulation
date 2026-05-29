package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;
import model.strategies.PassiveStrategy;

public class Rabbit extends HerbivoreAnimal {

    public Rabbit(Vector2 position) {
        // Khởi tạo thỏ với kích thước 20.0f và tốc độ lấy từ GameConfig
        super(position, 20.0f, GameConfig.getInstance().RABBIT_BASE_SPEED);

        // Khởi tạo các chỉ số sinh học
        this.speciesName = "Thỏ";
        this.maxHealth = 100.0f;
        this.health = this.maxHealth;
        this.maxHunger = 100.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 2.0f; // Tiêu hao 2.0 đói mỗi giây khi đứng yên
        this.maxThirst = 100.0f;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 3.0f; // Tiêu hao 3.0 khát mỗi giây khi đứng yên (Thỏ nhanh khát hơn đói)
        this.maxAge = 300.0f; // Sống tối đa 5 phút mô phỏng

        // Gắn "bộ não" mặc định cho Thỏ ở Phase 1 là đi dạo/đứng im (Passive)
        this.setStrategy(new PassiveStrategy());
    }

    @Override
    public void render(DisplayMode mode) {
        // Hàm này để trống vì RenderSystem sẽ đảm nhận việc vẽ hình ảnh
    }
}
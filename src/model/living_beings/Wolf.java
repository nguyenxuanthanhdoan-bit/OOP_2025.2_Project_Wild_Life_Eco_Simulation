package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;
import model.strategies.PassiveStrategy;

public class Wolf extends CarnivoreAnimal {

    public Wolf(Vector2 position) {
        // Khởi tạo sói với kích thước 28.0f và tốc độ lấy từ GameConfig
        super(position, 28.0f, GameConfig.getInstance().WOLF_BASE_SPEED);

        // Khởi tạo các chỉ số sinh học
        this.speciesName = "Sói";
        this.maxHealth = 150.0f;
        this.health = this.maxHealth;
        this.maxHunger = 120.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 1.8f;
        this.maxThirst = 100.0f;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 2.5f;
        this.maxAge = 450.0f;

        // Gắn "bộ não" mặc định cho Sói ở Phase 1 là đi dạo/đứng im (Passive)
        this.setStrategy(new PassiveStrategy());
    }

    @Override
    public void render(DisplayMode mode) {
        // Hàm này để trống vì RenderSystem sẽ đảm nhận việc vẽ hình ảnh
    }
}

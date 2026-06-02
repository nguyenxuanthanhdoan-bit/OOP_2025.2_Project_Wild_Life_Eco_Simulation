package model.living_beings;

import core.Vector2;
import core.GameConfig;
import core.DisplayMode;
import model.strategies.PassiveStrategy;

public class Tiger extends CarnivoreAnimal {

    public Tiger(Vector2 position) {
        // Khởi tạo hổ với kích thước 35.0f và tốc độ lấy từ GameConfig
        super(position, 35.0f, GameConfig.getInstance().TIGER_BASE_SPEED);

        // Khởi tạo các chỉ số sinh học
        this.speciesName = "Hổ";
        this.maxHealth = 250.0f;
        this.health = this.maxHealth;
        this.maxHunger = 200.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 1.5f; // Ăn no lâu
        this.maxThirst = 150.0f;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 2.0f; 
        this.maxAge = 600.0f; // Sống thọ hơn

        // Gắn "bộ não" mặc định cho Hổ ở Phase 1 là đi dạo/đứng im (Passive)
        this.setStrategy(new model.strategies.HunterStrategy());
    }

    @Override
    public Animal reproduce() {
        return null;
    }

    @Override
    public void render(DisplayMode mode) {
        // Hàm này để trống vì RenderSystem sẽ đảm nhận việc vẽ hình ảnh
    }
}

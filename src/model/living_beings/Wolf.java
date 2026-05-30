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
    public Animal reproduce() {
        if (!canReproduce()) return null;
        if (world == null) return null;

        float offsetX = (new java.util.Random().nextFloat() * 60) - 30;
        float offsetY = (new java.util.Random().nextFloat() * 60) - 30;
        Vector2 birthPos = new Vector2(this.position.x + offsetX, this.position.y + offsetY);

        Wolf offspring = new Wolf(birthPos);

        this.hunger = Math.max(0, this.hunger - maxHunger * 0.3);
        this.thirst = Math.max(0, this.thirst - maxThirst * 0.2);
        this.health = Math.max(1, this.health - maxHealth * 0.1);

        System.out.printf("[%s] sinh ra một con Sói con tại %s%n", speciesName, birthPos);
        return offspring;
    }

    @Override
    public void render(DisplayMode mode) {
        // Hàm này để trống vì RenderSystem sẽ đảm nhận việc vẽ hình ảnh
    }
}

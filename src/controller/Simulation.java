package controller;

import model.world.World;
import view.systems.RenderSystem;
import view.systems.Camera;
import core.DisplayMode;
import java.awt.Graphics2D;

/**
 * Trái tim điều phối toàn bộ hệ thống[cite: 206].
 */
public class Simulation {

    private World world;
    private RenderSystem renderSystem;
    private Camera camera;
    private DisplayMode currentDisplayMode;

    public Simulation(Camera camera, World world, RenderSystem renderSystem) {
        this.camera = camera;
        this.world = world;
        this.renderSystem = renderSystem;
        this.currentDisplayMode = DisplayMode.REALISTIC;
    }

    /**
     * Cập nhật logic và hiển thị trong mỗi khung hình [cite: 208-209].
     */
    public void update(float deltaTime) {
        // 1. Cập nhật logic thế giới (Thỏ di chuyển, cây cỏ...) [cite: 208]
        world.update(deltaTime);

        // Lưu ý: Việc gọi vẽ thực tế sẽ được Main gọi thông qua Graphics2D
    }

    public void render(Graphics2D g2d, float deltaTime) {
        // 2. Vẽ thế giới lên màn hình [cite: 209]
        renderSystem.renderAll(world, g2d, deltaTime);
    }

    public void toggleDisplayMode() {
        if (currentDisplayMode == DisplayMode.REALISTIC) {
            currentDisplayMode = DisplayMode.MINIMAL;
        } else {
            currentDisplayMode = DisplayMode.REALISTIC;
        }
        renderSystem.setDisplayMode(currentDisplayMode);
    }

    // Các hàm như adjustWeather(), changeMap() sẽ được thêm vào ở Phase sau [cite: 208]
}
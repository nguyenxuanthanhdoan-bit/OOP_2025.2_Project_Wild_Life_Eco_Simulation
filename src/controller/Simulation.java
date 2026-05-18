package controller;

import model.world.World;
import model.map.GameMap; // Nhớ import GameMap
import view.systems.RenderSystem;
import view.systems.Camera;
import core.DisplayMode;
import java.awt.Graphics2D;
import core.Vector2;

/**
 * Trái tim điều phối toàn bộ hệ thống.
 */
public class Simulation {

    private World world;
    private RenderSystem renderSystem;
    private Camera camera;
    private DisplayMode currentDisplayMode;

    // [MỚI] Thêm biến quản lý bản đồ
    private GameMap gameMap;

    public Simulation(Camera camera, World world, RenderSystem renderSystem) {
        this.camera = camera;
        this.world = world;
        this.renderSystem = renderSystem;
        this.currentDisplayMode = DisplayMode.REALISTIC;

        // [MỚI] Khởi tạo bản đồ ngay khi game bắt đầu
        initMap();
    }

    // Hàm chuyên lo việc nạp dữ liệu bản đồ
    private void initMap() {
        this.gameMap = new GameMap("resources/assets/images/world_map.png");
        this.renderSystem.setGameMap(this.gameMap);

        // 1. ĐỒNG BỘ KÍCH THƯỚC: Báo cho World biết thế giới này to bằng kích thước map
        // (Kích thước ảnh x 32 pixel mỗi ô)
        float realWorldWidth = gameMap.getCols() * 32f;
        float realWorldHeight = gameMap.getRows() * 32f;
        this.world.setWidth(realWorldWidth);
        this.world.setHeight(realWorldHeight);
        this.camera.setWorldBounds(realWorldWidth, realWorldHeight);
        // 2. DI CHUYỂN CAMERA RA GIỮA ĐẢO
        // Đặt camera ở chính giữa bản đồ thay vì góc (0,0)
        camera.setPosition(new Vector2(realWorldWidth / 2, realWorldHeight / 2));
    }

    /**
     * Cập nhật logic trong mỗi khung hình.
     */
    public void update(float deltaTime) {
        // 1. Cập nhật logic thế giới (Thỏ di chuyển, cây cỏ...)
        world.update(deltaTime);
    }

    public void render(Graphics2D g2d, float deltaTime) {
        // 2. Vẽ thế giới lên màn hình
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
}
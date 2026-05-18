package model.world;

import model.entity.Entity;
import core.GameConfig;
import java.util.ArrayList;
import java.util.List;
import core.Vector2;

/**
 * Lớp quản lý toàn bộ thực thể và môi trường của thế giới.
 */
public class World {

    private List<Entity> entities; // Danh sách các thực thể (Thỏ, Cây, Cỏ...)
    private Biome currentBiome;    // Địa hình hiện tại (Trong Phase 1 là Grassland)

    // Kích thước của thế giới lấy từ cấu hình
    private float width;
    private float height;

    public World() {
        this.entities = new ArrayList<>();
        this.width = GameConfig.getInstance().WORLD_WIDTH;
        this.height = GameConfig.getInstance().WORLD_HEIGHT;

        // Khởi tạo nền cỏ xanh bao phủ toàn bộ thế giới
        this.currentBiome = new Grassland(new core.Vector2(width/2, height/2), Math.max(width, height));
    }

    /**
     * Cập nhật toàn bộ logic của thế giới.
     */
    public void update(float deltaTime) {
        // Duyệt qua và cập nhật từng thực thể (ví dụ: Thỏ sẽ chạy nhảy)
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (e.isAlive()) {
                e.update(deltaTime);
                if (e instanceof model.living_beings.Rabbit) {
                    keepInBounds(e);
                }
            }
        }

        // Trong Phase 1, Biome chưa cần cập nhật logic
        currentBiome.update(deltaTime);
    }

    /**
     * Thêm một thực thể mới vào thế giới.
     */
    public void addEntity(Entity e) {
        if (!entities.contains(e)) {
            entities.add(e);
        }
    }
    /**
     * Đảm bảo thực thể không bao giờ vượt quá giới hạn thế giới
     */
    private void keepInBounds(Entity e) {
        Vector2 pos = e.getPosition(); // Lấy vị trí hiện tại
        float currentX = pos.x;
        float currentY = pos.y;
        float margin = e.getSize() / 2;

        boolean isOutOfBounds = false;

        // Chặn trục X
        if (currentX < margin) { currentX = margin; isOutOfBounds = true; }
        if (currentX > width - margin) { currentX = width - margin; isOutOfBounds = true; }

        // Chặn trục Y
        if (currentY < margin) { currentY = margin; isOutOfBounds = true; }
        if (currentY > height - margin) { currentY = height - margin; isOutOfBounds = true; }

        // Nếu phát hiện vượt rào, dùng hàm setPosition chuẩn của bạn để ép nó về
        if (isOutOfBounds) {
            e.setPosition(new core.Vector2(currentX, currentY));
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public List<Entity> getEntities() {
        return entities;
    }

    public Biome getCurrentBiome() {
        return currentBiome;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
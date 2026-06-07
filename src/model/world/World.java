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

    // Kích thước của thế giới
    private float width;
    private float height;

    // [MỚI] Quản lý lưới không gian
    private SpatialGrid spatialGrid;

    // [MỚI] Tham chiếu đến GameMap
    private model.map.GameMap gameMap;

    public World() {
        this.entities = new ArrayList<>();
        this.width = GameConfig.getInstance().WORLD_WIDTH;
        this.height = GameConfig.getInstance().WORLD_HEIGHT;

        // Khởi tạo nền cỏ xanh bao phủ toàn bộ thế giới
        this.currentBiome = new Grassland(new core.Vector2(width/2, height/2), Math.max(width, height));

        // [MỚI] Khởi tạo lưới ngay từ đầu nếu đã có kích thước từ Config
        checkAndInitGrid();
    }

    // [MỚI] Hàm khởi tạo Lưới (Dùng chung cho Constructor và Setters)
    private void checkAndInitGrid() {
        if (this.width > 0 && this.height > 0) {
            // Khởi tạo lưới với ô cỡ 256px
            this.spatialGrid = new SpatialGrid(this.width, this.height, 256f);

            // Nếu có thực thể nào lỡ sinh ra trước khi có lưới, nạp bù nó vào
            for (Entity e : this.entities) {
                this.spatialGrid.add(e);
            }
        }
    }

    /**
     * Cập nhật toàn bộ logic của thế giới.
     */
    public void update(float deltaTime) {
        // [MỚI] Dùng vòng lặp ngược hoặc quản lý chỉ số cẩn thận khi có thể xóa phần tử
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);

            if (e.isAlive()) {
                // [MỚI] Lưu lại tọa độ CŨ trước khi thực thể di chuyển
                Vector2 oldPos = null;
                if (e.getPosition() != null) {
                    oldPos = new Vector2(e.getPosition().x, e.getPosition().y);
                }

                // Gán worldRef để StuckDetector có thể quét SpatialGrid
                if (e instanceof model.living_beings.Animal) {
                    ((model.living_beings.Animal) e).setWorldRef(this);
                }

                // Cập nhật logic (Thỏ chạy nhảy, chuyển sang tọa độ MỚI)
                e.update(deltaTime);

                if (e instanceof model.living_beings.LivingBeing) {
                    keepInBounds(e);
                }

                // [MỚI] Báo cho Lưới biết để kiểm tra xem thực thể có bước sang ô khác không
                if (this.spatialGrid != null && oldPos != null) {
                    this.spatialGrid.updateEntityPosition(e, oldPos);
                }
            } else {
                // [MỚI] Nếu thực thể đã chết, xóa nó khỏi map và lưới
                removeEntity(e);
                i--; // Lùi index lại để không bị bỏ sót phần tử tiếp theo
            }
        }

        // Trong Phase 1, Biome chưa cần cập nhật logic
        currentBiome.update(deltaTime);
    }

    public void addEntity(Entity e) {
        if (!entities.contains(e)) {
            e.setWorld(this); // Liên kết World vào Entity
            entities.add(e);
            // [MỚI] Đồng bộ: Thêm vào danh sách tổng xong thì ném luôn vào Lưới
            if (this.spatialGrid != null) {
                this.spatialGrid.add(e);
            }
        }
    }

    /**
     * [MỚI] Hàm xóa thực thể khỏi thế giới
     */
    public void removeEntity(Entity e) {
        if (entities.contains(e)) {
            entities.remove(e);
            // Đồng bộ: Rút thực thể ra khỏi Lưới
            if (this.spatialGrid != null) {
                this.spatialGrid.remove(e);
            }
        }
    }

    private void keepInBounds(Entity e) {
        Vector2 pos = e.getPosition();
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

        if (isOutOfBounds) {
            e.setPosition(new core.Vector2(currentX, currentY));
        }
    }

    public boolean isPositionInWater(float x, float y) {
        if (this.gameMap != null) {
            return this.gameMap.isPositionInWater(x, y);
        }
        return false;
    }

    public boolean isValidGroundSpawnPosition(float x, float y, float margin) {
        if (this.gameMap != null) {
            return this.gameMap.isValidGroundSpawnPosition(x, y, margin);
        }
        return !isPositionInWater(x, y);
    }

    public boolean isValidPositionFor(model.living_beings.LivingBeing entity, Vector2 pos) {
        float margin = entity.getSize() / 2;

        // Kiểm tra ranh giới bản đồ
        if (pos.x < margin || pos.x > width - margin ||
            pos.y < margin || pos.y > height - margin) {
            return false;
        }

        // Kiểm tra địa hình nước đối với động vật trên cạn
        if (gameMap != null) {
            // Nếu động vật đứng trên cầu -> luôn cho phép, dù xung quanh là nước
            if (gameMap.isBridgeTile(pos.x, pos.y)) {
                return true;
            }

            // Kiểm tra chính xác vị trí hiện tại và các góc của hitbox động vật
            float m = entity.getSize() / 2;
            boolean inWater = gameMap.isPositionInWater(pos.x,     pos.y)     ||
                              gameMap.isPositionInWater(pos.x - m, pos.y - m) ||
                              gameMap.isPositionInWater(pos.x + m, pos.y - m) ||
                              gameMap.isPositionInWater(pos.x - m, pos.y + m) ||
                              gameMap.isPositionInWater(pos.x + m, pos.y + m);
            if (inWater) return false;
        }



        return true;
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public List<Entity> getEntities() {
        return entities;
    }

    // [MỚI] Getter cho Lưới Không Gian (Để RenderSystem gọi tới)
    public SpatialGrid getSpatialGrid() {
        return spatialGrid;
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

    public void setGameMap(model.map.GameMap gameMap) {
        this.gameMap = gameMap;
    }

    public void setWidth(float width) {
        this.width = width;
        checkAndInitGrid(); // [MỚI] Cập nhật lại lưới nếu kích thước map bị đổi qua code đọc ảnh
    }

    public void setHeight(float height) {
        this.height = height;
        checkAndInitGrid(); // [MỚI]
    }
}

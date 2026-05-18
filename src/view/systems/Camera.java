package view.systems;

import core.Vector2;
import core.GameConfig;

/**
 * Quản lý khung hình nhìn vào thế giới.
 * Hỗ trợ di chuyển (Pan), Thu phóng (Zoom) và giới hạn trong bản đồ.
 */
public class Camera {

    private float worldX; // Tọa độ X của góc trái-trên camera trong thế giới
    private float worldY; // Tọa độ Y của góc trái-trên camera trong thế giới
    private float zoomLevel;

    private float viewportWidth;  // Chiều rộng cửa sổ màn hình
    private float viewportHeight; // Chiều cao cửa sổ màn hình

    // [MỚI] Quản lý biên giới bản đồ động thay vì dùng trực tiếp GameConfig cố định
    private float worldWidth;
    private float worldHeight;

    public Camera(float viewportWidth, float viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.zoomLevel = 1.0f;
        this.worldX = 0;
        this.worldY = 0;

        // Khởi tạo biên mặc định từ cấu hình
        GameConfig config = GameConfig.getInstance();
        this.worldWidth = config.WORLD_WIDTH;
        this.worldHeight = config.WORLD_HEIGHT;
    }

    /**
     * [MỚI] Cập nhật kích thước vùng nhìn thực tế khi cửa sổ game bị phóng to/thu nhỏ.
     */
    public void setViewportSize(float width, float height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        clamp();
    }

    /**
     * [MỚI] Cập nhật biên giới thế giới động dựa theo kích thước của tấm ảnh Map được nạp.
     */
    public void setWorldBounds(float width, float height) {
        this.worldWidth = width;
        this.worldHeight = height;
        clamp();
    }

    /**
     * Chuyển đổi tọa độ từ Thế giới sang Màn hình để vẽ.
     */
    public Vector2 worldToScreen(Vector2 worldPos) {
        float screenX = (worldPos.x - worldX) * zoomLevel;
        float screenY = (worldPos.y - worldY) * zoomLevel;
        return new Vector2(screenX, screenY);
    }

    /**
     * Chuyển đổi tọa độ từ Màn hình ngược lại Thế giới (dùng cho click chuột).
     */
    public Vector2 screenToWorld(Vector2 screenPos) {
        float worldXCoord = (screenPos.x / zoomLevel) + worldX;
        float worldYCoord = (screenPos.y / zoomLevel) + worldY;
        return new Vector2(worldXCoord, worldYCoord);
    }


    /**
     * Di chuyển camera (WASD).
     */
    public void pan(float dx, float dy) {
        // Chia cho zoomLevel để bù đắp tốc độ.
        // Khi zoom < 1 (thu nhỏ), phép chia sẽ làm dx, dy lớn lên -> Cuộn map cực nhanh!
        this.worldX += (dx / zoomLevel);
        this.worldY += (dy / zoomLevel);
        clamp();
    }

    /**
     * Thu phóng camera (Mũi tên lên/xuống).
     */
    public void zoom(float factor) {
        this.zoomLevel *= factor;

        GameConfig config = GameConfig.getInstance();
        if (this.zoomLevel > config.MAX_ZOOM) this.zoomLevel = config.MAX_ZOOM;
        if (this.zoomLevel < config.MIN_ZOOM) this.zoomLevel = config.MIN_ZOOM;

        clamp();
    }

    /**
     * Ràng buộc camera không được vượt quá giới hạn bản đồ.
     */
    private void clamp() {
        // Chiều rộng/cao của "vùng nhìn" tính theo đơn vị thế giới
        float viewWidthInWorld = viewportWidth / zoomLevel;
        float viewHeightInWorld = viewportHeight / zoomLevel;

        // Chặn biên trái và biên trên
        if (worldX < 0) worldX = 0;
        if (worldY < 0) worldY = 0;

        // [SỬA THÀNH BIẾN ĐỘNG] Chặn biên phải và biên dưới theo map thực tế
        if (worldX + viewWidthInWorld > worldWidth) {
            worldX = worldWidth - viewWidthInWorld;
        }
        if (worldY + viewHeightInWorld > worldHeight) {
            worldY = worldHeight - viewHeightInWorld;
        }

        // Trường hợp đặt biệt: Nếu kích thước map nhỏ hơn cả vùng nhìn màn hình
        if (viewWidthInWorld > worldWidth) worldX = (worldWidth - viewWidthInWorld) / 2f;
        if (viewHeightInWorld > worldHeight) worldY = (worldHeight - viewHeightInWorld) / 2f;
    }

    /**
     * Kiểm tra xem một thực thể có nằm trong màn hình không để tối ưu Render.
     */
    public boolean isVisible(Vector2 pos, float size) {
        float viewWidthInWorld = viewportWidth / zoomLevel;
        float viewHeightInWorld = viewportHeight / zoomLevel;

        return pos.x + size >= worldX && pos.x - size <= worldX + viewWidthInWorld &&
                pos.y + size >= worldY && pos.y - size <= worldY + viewHeightInWorld;
    }

    // Getters & Setters
    public float getZoomLevel() { return zoomLevel; }
    public Vector2 getPosition() { return new Vector2(worldX, worldY); }

    public void setPosition(Vector2 worldCenter) {
        if (worldCenter == null) return;

        float viewWidthInWorld = viewportWidth / zoomLevel;
        float viewHeightInWorld = viewportHeight / zoomLevel;

        this.worldX = worldCenter.x - (viewWidthInWorld / 2f);
        this.worldY = worldCenter.y - (viewHeightInWorld / 2f);

        clamp();
    }

    public Vector2 setPositon(Vector2 worldCenter) {
        setPosition(worldCenter);
        return getPosition();
    }
}
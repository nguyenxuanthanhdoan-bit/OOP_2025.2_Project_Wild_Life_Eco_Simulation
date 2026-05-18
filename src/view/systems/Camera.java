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

    private float viewportWidth;  // Chiều rộng cửa sổ màn hình (ví dụ: 800)
    private float viewportHeight; // Chiều cao cửa sổ màn hình (ví dụ: 600)

    public Camera(float viewportWidth, float viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.zoomLevel = 1.0f;
        this.worldX = 0;
        this.worldY = 0;
    }

    /**
     * Chuyển đổi tọa độ từ Thế giới sang Màn hình để vẽ.
     * Công thức: (Tọa độ thế giới - Tọa độ Camera) * Zoom.
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
        this.worldX += dx;
        this.worldY += dy;
        clamp(); // Chặn kịch viền ngay sau khi di chuyển
    }

    /**
     * Thu phóng camera (Mũi tên lên/xuống).
     */
    public void zoom(float factor) {
        float oldZoom = zoomLevel;
        this.zoomLevel *= factor;

        // Giới hạn zoom từ GameConfig
        GameConfig config = GameConfig.getInstance();
        if (this.zoomLevel > config.MAX_ZOOM) this.zoomLevel = config.MAX_ZOOM;
        if (this.zoomLevel < config.MIN_ZOOM) this.zoomLevel = config.MIN_ZOOM;

        // Khi zoom, cần điều chỉnh lại worldX, worldY để zoom vào tâm màn hình
        // (Đây là logic nâng cao, tạm thời ta clamp lại để tránh lệch model.map)
        clamp();
    }

    /**
     * Ràng buộc camera không được vượt quá giới hạn bản đồ.
     */
    private void clamp() {
        GameConfig config = GameConfig.getInstance();

        // Chiều rộng/cao của "vùng nhìn" tính theo đơn vị thế giới
        float viewWidthInWorld = viewportWidth / zoomLevel;
        float viewHeightInWorld = viewportHeight / zoomLevel;

        // Chặn biên trái và biên trên
        if (worldX < 0) worldX = 0;
        if (worldY < 0) worldY = 0;

        // Chặn biên phải và biên dưới
        if (worldX + viewWidthInWorld > config.WORLD_WIDTH) {
            worldX = config.WORLD_WIDTH - viewWidthInWorld;
        }
        if (worldY + viewHeightInWorld > config.WORLD_HEIGHT) {
            worldY = config.WORLD_HEIGHT - viewHeightInWorld;
        }

        // Trường hợp đặc biệt: Nếu model.map nhỏ hơn cả màn hình khi zoom out quá xa
        if (viewWidthInWorld > config.WORLD_WIDTH) worldX = (config.WORLD_WIDTH - viewWidthInWorld) / 2;
        if (viewHeightInWorld > config.WORLD_HEIGHT) worldY = (config.WORLD_HEIGHT - viewHeightInWorld) / 2;
    }

    /**
     * Kiểm tra xem một thực thể có nằm trong màn hình không để tối ưu Render[cite: 183].
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
}
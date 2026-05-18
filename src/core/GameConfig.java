package core;

/**
 * Nơi tập trung toàn bộ các hằng số cấu hình của game.
 * Trong Phase 1, dùng để giới hạn bản đồ và tốc độ thực thể/camera.
 */
public class GameConfig {

    // Áp dụng Singleton Pattern để truy cập cấu hình ở mọi nơi mà không cần khởi tạo lại
    private static GameConfig instance;

    // ==========================================
    // CẤU HÌNH THẾ GIỚI (WORLD)
    // ==========================================
    // Giả sử bản đồ thực tế rộng 2000x2000 pixels (Camera của bạn có thể chỉ rộng 800x600)
    public final float WORLD_WIDTH = 8192.0f;
    public final float WORLD_HEIGHT = 7232.0f;

    // ==========================================
    // CẤU HÌNH CAMERA
    // ==========================================
    public final float CAMERA_PAN_SPEED = 400.0f; // Tốc độ lướt camera (pixels/giây)
    public final float CAMERA_ZOOM_SPEED = 2.0f;  // Tốc độ thu/phóng
    public final float MAX_ZOOM = 3.0f;           // Zoom out tối đa (nhìn được nhiều bản đồ hơn)
    public final float MIN_ZOOM = 0.2f;           // Zoom in tối đa (nhìn sát con thỏ)

    // ==========================================
    // CẤU HÌNH THỰC THỂ (PHASE 1)
    // ==========================================
    public final float RABBIT_BASE_SPEED = 100.0f; // Tốc độ chạy của thỏ (pixels/giây)

    // Constructor private để cấm dùng từ khóa 'new' từ bên ngoài
    private GameConfig() {}

    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }
}
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
    public final float TIGER_BASE_SPEED = 130.0f;  // Tốc độ chạy của hổ (pixels/giây)
    public final float WOLF_BASE_SPEED = 140.0f;   // Tốc độ chạy của sói (pixels/giây)

    // ==========================================
    // CẤU HÌNH GAME LOOP
    // ==========================================
    public final int TARGET_FPS = 60;
    public final float MAX_DELTA_TIME = 0.05f;

    // ==========================================
    // CẤU HÌNH MAP / TILE
    // ==========================================
    public final String DEFAULT_MAP_PATH = "resources/map/map2.tmx";
    public final int TILE_SIZE = 32;
    public final float GROUND_SPAWN_MARGIN = 32.0f;

    // ==========================================
    // CẤU HÌNH SPAWN BAN ĐẦU
    // ==========================================
    public final int INITIAL_GRASS_COUNT = 320;
    public final float GRASS_PLAIN_SPAWN_CHANCE = 0.9f;
    public final int INITIAL_MUSHROOM_COUNT = 60;
    public final float MUSHROOM_PLAIN_SPAWN_CHANCE = 0.2f;
    public final int INITIAL_PLAIN_TREE_COUNT = 30;
    public final int INITIAL_FOREST_TREE_COUNT = 400;
    public final int INITIAL_BUSH_COUNT = 80;
    public final float BUSH_PLAIN_SPAWN_CHANCE = 0.3f;
    public final int INITIAL_ROCK_COUNT = 30;
    public final float ROCK_PLAIN_SPAWN_CHANCE = 0.5f;
    public final int MIN_HOUSES_PER_VILLAGE = 7;
    public final int MAX_HOUSES_PER_VILLAGE = 7;
    public final int WELLS_PER_VILLAGE = 2;
    public final int FOOD_STORAGES_PER_VILLAGE = 1;
    public final int DECORATIONS_PER_VILLAGE = 10;
    public final int HOUSE_CAPACITY = 6;
    public final float HOUSE_SIZE = 64.0f;
    public final float WELL_SIZE = 54.0f;
    public final float FOOD_STORAGE_SIZE = 58.0f;
    public final float FOOD_STORAGE_CAPACITY = 600.0f;
    public final float DECORATIVE_STRUCTURE_SIZE = 50.0f;
    public final float VILLAGE_STRUCTURE_MIN_DISTANCE = 70.0f;
    public final float VILLAGE_STRUCTURE_CLUSTER_RADIUS = 200.0f;
    public final int HUMANS_PER_VILLAGE = 14;
    public final int HUNTERS_PER_VILLAGE = 2;
    public final float HUMAN_CARRY_CAPACITY = 45.0f;
    public final float HUNTER_CARRY_CAPACITY = 180.0f;
    public final float VILLAGE_HOME_RADIUS_PADDING = 80.0f;
    public final int MAX_INITIAL_ANIMAL_COUNT = 250;
    public final int SPAWN_ATTEMPTS_PER_POINT = 180;
    public final int SUPPLEMENTAL_SPAWN_ATTEMPT_MULTIPLIER = 30;
    public final double INITIAL_SPAWN_MIN_AGE_RATIO = 0.25;
    public final double INITIAL_SPAWN_MAX_AGE_RATIO = 0.65;
    public final float HUMAN_BASE_SPEED = 85.0f;
    public final float HUNTER_BASE_SPEED = 95.0f;
    public final int HUNTER_MAX_AMMO = 8;
    public final float HUNTER_SHOOT_RANGE = 240.0f;
    public final float HUNTER_FIRE_COOLDOWN_SECONDS = 0.75f;
    public final float HUNTER_RELOAD_SECONDS = 2.0f;
    public final float HUNTER_PROJECTILE_SPEED = 430.0f;
    public final float HUNTER_PROJECTILE_SIZE = 12.0f;
    public final float HUNTER_PROJECTILE_DAMAGE = 45.0f;
    public final float HUNTER_RETURN_FOOD_RATIO = 0.75f;

    // ==========================================
    // CẤU HÌNH KHU DÂN CƯ & NGỦ (SETTLEMENT & SLEEP)
    // ==========================================
    /** Khoảng cách tối đa để Human có thể bước vào nhà (pixels). */
    public final float HOUSE_ENTER_DISTANCE = 72.0f;
    /** Bán kính phát hiện mối đe dọa của Human (pixels). */
    public final float THREAT_RADIUS = 300.0f;
    /**
     * Padding thêm vào homeRadius khi tính vùng Safe Zone của Settlement.
     * safeRadius = villagePolygonRadius + SETTLEMENT_SAFE_RADIUS_PADDING
     */
    public final float SETTLEMENT_SAFE_RADIUS_PADDING = 100.0f;

    // ==========================================
    // CẤU HÌNH VƯỜN (GARDEN)
    // ==========================================
    public final float GARDEN_BED_SIZE = 40.0f;
    public final boolean ALLOW_DEER_ENTER_GARDEN = false;

    // ==========================================
    // CẤU HÌNH VEN BIỂN (COASTAL / FISHING)
    // ==========================================
    /** Số thuyền cần spawn trên biển. */
    public final int BOAT_COUNT = 10;
    /** Số nhà chài cần spawn gần mép biển. */
    public final int FISHING_HUT_COUNT = 10;
    /**
     * Khoảng cách tối thiểu từ mép nước để tìm vị trí đặt nhà chài (pixels).
     * Nhà chài nằm trên đất nhưng sát biển.
     */
    public final float FISHING_HUT_SHORE_MAX_DIST = 80.0f;
    /** Khoảng cách tối thiểu giữa các nhà chài (pixels). */
    public final float FISHING_HUT_MIN_DISTANCE = 150.0f;
    /** Khoảng cách tối thiểu giữa các thuyền (pixels). */
    public final float BOAT_MIN_DISTANCE = 120.0f;
    /**
     * Xác suất (0-1) Human ban ngày chọn đích là điểm ven biển thay vì lang thang trong làng.
     * 0.35 = 35% mỗi lần chọn đích mới sẽ hướng ra biển.
     */
    public final float HUMAN_COASTAL_VISIT_CHANCE = 0.35f;

    // ==========================================
    // CẤU HÌNH QUẦN THỂ / SINH SẢN
    // ==========================================
    public final int MIN_SPECIES_POPULATION = 15;
    public final int MAX_SPECIES_POPULATION = 100;
    public final int MAX_ANIMAL_POPULATION = 200;
    public final int POPULATION_RESPAWN_ATTEMPTS = 30;
    public final float POPULATION_SAFE_SPAWN_PREDATOR_RADIUS = 500.0f;
    public final float MATING_DURATION_SECONDS = 2.0f;
    public final float REPRODUCTION_COOLDOWN_SECONDS = 60.0f;
    public final double REPRODUCTION_ENERGY_COST = 50.0;

    // ==========================================
    // CẤU HÌNH RENDER / HUD
    // ==========================================
    public final float ANIMATION_FRAME_DURATION = 0.15f;
    public final int MINIMAP_MARGIN = 14;
    public final int MINIMAP_SIZE = 190;
    public final int MINIMAP_MIN_HEIGHT = 80;
    public final float STATUS_BAR_MIN_ZOOM = 0.65f;

    // Constructor private để cấm dùng từ khóa 'new' từ bên ngoài
    private GameConfig() {}

    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }
}

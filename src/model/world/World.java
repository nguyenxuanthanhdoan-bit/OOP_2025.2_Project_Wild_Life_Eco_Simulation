package model.world;

import model.entity.Entity;
import core.GameConfig;
import model.living_beings.Animal;

import java.util.ArrayList;
import java.util.List;
import core.Vector2;

/**
 * Lớp quản lý toàn bộ thực thể và môi trường của thế giới.
 */
public class World {

    public enum Season {
        SPRING("Xuân"), SUMMER("Hạ"), AUTUMN("Thu"), WINTER("Đông");
        private final String name;
        Season(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public enum Weather {
        SUNNY("Nắng đẹp"), RAINY("Mưa rào"), SNOWY("Tuyết rơi"), STORMY("Bão bùng");
        private final String name;
        Weather(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private float dayTimer = 0.0f;
    private int gameDay = 1;
    private Season currentSeason = Season.SPRING;
    private Weather currentWeather = Weather.SUNNY;
    private float weatherTimer = 0.0f;

    private List<Entity> entities; // Danh sách các thực thể (Thỏ, Cây, Cỏ...)
    private Biome currentBiome;    // Địa hình hiện tại (Trong Phase 1 là Grassland)

    // Kích thước của thế giới
    private float width;
    private float height;

    // [MỚI] Quản lý lưới không gian
    private SpatialGrid spatialGrid;

    // [MỚI] Tham chiếu đến GameMap
    private model.map.GameMap gameMap;
    private final WorldEventSystem eventSystem;

    public World() {
        this.entities = new ArrayList<>();
        this.eventSystem = new WorldEventSystem();
        this.width = GameConfig.getInstance().WORLD_WIDTH;
        this.height = GameConfig.getInstance().WORLD_HEIGHT;
        registerDefaultEventListeners();

        // Khởi tạo nền cỏ xanh bao phủ toàn bộ thế giới
        this.currentBiome = new Grassland(new core.Vector2(width/2, height/2), Math.max(width, height));

        // [MỚI] Khởi tạo lưới ngay từ đầu nếu đã có kích thước từ Config
        checkAndInitGrid();
    }

    private void registerDefaultEventListeners() {
        eventSystem.subscribe(WorldEventType.ANIMAL_DIED, event -> {
            if (event.getEntity() instanceof Animal) {
                PopulationManager.onAnimalDeath((Animal) event.getEntity(), this);
            }
        });
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
        // Progress day/season/weather
        dayTimer += deltaTime;
        if (dayTimer >= 60.0f) { // 60 seconds per day
            dayTimer = 0.0f;
            gameDay++;
            if (gameDay % 5 == 1) {
                nextSeason();
            }
        }

        weatherTimer += deltaTime;
        if (weatherTimer >= 30.0f) { // Change weather every 30 seconds
            weatherTimer = 0.0f;
            changeRandomWeather();
        }

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
            publishEvent(WorldEventType.ENTITY_ADDED, e, "addEntity");
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
            publishEvent(WorldEventType.ENTITY_REMOVED, e, "removeEntity");
        }
    }

    public void publishEvent(WorldEventType type, Entity entity, String reason) {
        eventSystem.emit(new WorldEvent(type, entity, this, reason));
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

    public WorldEventSystem getEventSystem() {
        return eventSystem;
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

    public void nextSeason() {
        Season[] seasons = Season.values();
        int nextIdx = (currentSeason.ordinal() + 1) % seasons.length;
        setSeason(seasons[nextIdx]);
    }

    public void setSeason(Season season) {
        this.currentSeason = season;
        publishEvent(WorldEventType.SEASON_CHANGED, null, "manual_change");
    }

    public void changeRandomWeather() {
        Weather[] weathers = Weather.values();
        java.util.Random rand = new java.util.Random();
        if (currentSeason == Season.WINTER) {
            int r = rand.nextInt(10);
            if (r < 6) this.currentWeather = Weather.SNOWY;
            else if (r < 8) this.currentWeather = Weather.STORMY;
            else this.currentWeather = Weather.RAINY;
        } else if (currentSeason == Season.SUMMER) {
            int r = rand.nextInt(10);
            if (r < 7) this.currentWeather = Weather.SUNNY;
            else if (r < 9) this.currentWeather = Weather.STORMY;
            else this.currentWeather = Weather.RAINY;
        } else {
            this.currentWeather = weathers[rand.nextInt(weathers.length)];
        }
    }

    public void setWeather(Weather weather) {
        this.currentWeather = weather;
    }

    public void reset() {
        this.entities.clear();
        if (this.spatialGrid != null) {
            this.spatialGrid.clear();
        }
        this.gameDay = 1;
        this.dayTimer = 0.0f;
        this.currentSeason = Season.SPRING;
        this.currentWeather = Weather.SUNNY;
        this.weatherTimer = 0.0f;
    }

    public float getDayTimer() { return dayTimer; }
    public int getGameDay() { return gameDay; }
    public Season getCurrentSeason() { return currentSeason; }
    public Weather getCurrentWeather() { return currentWeather; }
}

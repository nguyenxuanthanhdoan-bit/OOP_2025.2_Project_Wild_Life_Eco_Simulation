package model.world;

import model.entity.Entity;
import model.entity.Structure;
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

    // Thời gian trong ngày (0.0 đến 24.0)
    private float timeOfDay = 6.0f; // Bắt đầu từ 6 giờ sáng
    // 1 ngày game = 120s thực tế => 24h = 120s => 1h = 5s => tốc độ = 0.2
    private float timeScale = 0.2f;

    // [MỚI] Quản lý lưới không gian
    private SpatialGrid spatialGrid;

    // [MỚI] Tham chiếu đến GameMap
    private model.map.GameMap gameMap;
    private final WorldEventSystem eventSystem;
    private final FishPopulationManager fishPopulationManager;

    // Quản lý khu dân cư (Settlement System)
    private final SettlementManager settlementManager = new SettlementManager();

    // Quản lý điểm thu hút ven biển (thuyền, nhà chài)
    private final CoastalManager coastalManager = new CoastalManager();

    // Quản lý các mảnh vườn (Garden)
    private final model.garden.CropManager cropManager = new model.garden.CropManager();

    public World() {
        this.entities = new ArrayList<>();
        this.eventSystem = new WorldEventSystem();
        this.fishPopulationManager = new FishPopulationManager(this);
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

        // Cập nhật cây trồng trong vườn
        cropManager.update(deltaTime);

        fishPopulationManager.update(deltaTime);

        // Cập nhật thời gian trong ngày
        timeOfDay += deltaTime * timeScale;
        if (timeOfDay >= 24.0f) {
            timeOfDay -= 24.0f;
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

    public boolean isBridgeTile(float x, float y) {
        if (this.gameMap != null) {
            return this.gameMap.isBridgeTile(x, y);
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

        if (collidesWithSolidStructure(entity, pos)) {
            return false;
        }

        // Chặn động vật nguy hiểm vào vùng dân cư (Settlement Safe Zone)
        if (isAnimalBlockedFromSettlement(entity) && settlementManager.isInsideSettlement(pos)) {
            return false;
        }

        // [MỚI] Chặn động vật dẫm lên vườn
        if (isAnimalBlockedFromGarden(entity) && cropManager.isInsideGarden(pos)) {
            return false;
        }

        // Phân loại logic cho động vật dưới nước và trên cạn
        if (gameMap != null) {
            if (entity instanceof model.living_beings.Fish) {
                // Động vật dưới nước (Cá): Chỉ cần vị trí là nước
                float m = entity.getSize() / 2;
                boolean notInWater = !gameMap.isPositionInWater(pos.x,     pos.y)     ||
                                     !gameMap.isPositionInWater(pos.x - m, pos.y - m) ||
                                     !gameMap.isPositionInWater(pos.x + m, pos.y - m) ||
                                     !gameMap.isPositionInWater(pos.x - m, pos.y + m) ||
                                     !gameMap.isPositionInWater(pos.x + m, pos.y + m);
                if (notInWater) return false;
                return true;
            } else {
                // Động vật trên cạn
                if (gameMap.isBridgeTile(pos.x, pos.y)) {
                    return true;
                }

                // Kiểm tra chính xác vị trí hiện tại và các góc của hitbox động vật
                float m = entity.getSize() / 2;
                
                // Trả về true nếu vị trí là nước nhưng KHÔNG phải cầu
                boolean inWater = (gameMap.isPositionInWater(pos.x,     pos.y)     && !gameMap.isBridgeTile(pos.x,     pos.y))     ||
                                  (gameMap.isPositionInWater(pos.x - m, pos.y - m) && !gameMap.isBridgeTile(pos.x - m, pos.y - m)) ||
                                  (gameMap.isPositionInWater(pos.x + m, pos.y - m) && !gameMap.isBridgeTile(pos.x + m, pos.y - m)) ||
                                  (gameMap.isPositionInWater(pos.x - m, pos.y + m) && !gameMap.isBridgeTile(pos.x - m, pos.y + m)) ||
                                  (gameMap.isPositionInWater(pos.x + m, pos.y + m) && !gameMap.isBridgeTile(pos.x + m, pos.y + m));
                if (inWater) return false;
            }
        }
        return true;
    }

    /**
     * Kiểm tra xem entity có phải là loài bị chặn khỏi khu dân cư không.
     *
     * Các loài bị chặn:
     *   - Wolf, Tiger (LEVEL_APEX_ANIMAL / LEVEL_CARNIVORE)
     *   - Elephant (entityLevel >= LEVEL_CARNIVORE)
     *   - Deer (entityLevel LEVEL_HERBIVORE nhưng không phải Human)
     *
     * Thiết kế không hard-code tên loài:
     *   - Human (và subclass) luôn được phép đi vào làng.
     *   - Các loài thủy sinh (Fish) không liên quan đến settlement.
     *   - Mọi Animal trên cạn không phải Human đều bị chặn nếu entityLevel >= LEVEL_HERBIVORE.
     */
    private boolean isAnimalBlockedFromSettlement(model.living_beings.LivingBeing entity) {
        if (entity instanceof model.living_beings.Human) return false; // Human luôn được vào
        if (entity instanceof model.living_beings.Fish)  return false; // Cá không liên quan
        if (entity instanceof model.living_beings.Animal) {
            // Tất cả Animal trên cạn không phải Human đều bị chặn
            // (Wolf/Tiger/Elephant: CARNIVORE/APEX; Deer: HERBIVORE)
            int level = entity.getEntityLevel();
            return level >= model.entity.Entity.LEVEL_HERBIVORE;
        }
        return false;
    }

    private boolean isAnimalBlockedFromGarden(model.living_beings.LivingBeing entity) {
        if (entity instanceof model.living_beings.Human) return false;
        if (entity instanceof model.living_beings.Fish) return false;
        if (entity instanceof model.living_beings.Deer) {
            return !GameConfig.getInstance().ALLOW_DEER_ENTER_GARDEN;
        }
        if (entity instanceof model.living_beings.Animal) {
            return true; // Tất cả thú dữ đều bị chặn khỏi vườn
        }
        return false;
    }

    private boolean collidesWithSolidStructure(model.living_beings.LivingBeing entity, Vector2 pos) {
        if (entity == null || pos == null || spatialGrid == null) return false;

        float entityRadius = entity.getCollider() != null
                ? entity.getCollider().getRadius()
                : entity.getSize() * 0.35f;
        List<Entity> nearby = spatialGrid.getNeighbors(pos, entityRadius + 80.0f);

        for (Entity other : nearby) {
            if (other == entity || !(other instanceof Structure) || !other.isSolid() || !other.isAlive()) {
                continue;
            }
            float otherRadius = other.getCollider() != null
                    ? other.getCollider().getRadius()
                    : other.getSize() * 0.35f;
            if (pos.distanceTo(other.getPosition()) < entityRadius + otherRadius) {
                return true;
            }
        }
        return false;
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

    /** Getter cho SettlementManager — dùng bởi GoHomeStrategy và BiomeGenerator. */
    public SettlementManager getSettlementManager() {
        return settlementManager;
    }

    /** Getter cho CoastalManager — dùng bởi PassiveStrategy (AI ban ngày) và BiomeGenerator. */
    public CoastalManager getCoastalManager() {
        return coastalManager;
    }

    public model.garden.CropManager getCropManager() {
        return cropManager;
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

    public model.map.GameMap getGameMap() {
        return gameMap;
    }

    public void setGameMap(model.map.GameMap gameMap) {
        this.gameMap = gameMap;
    }

    public float getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(float timeOfDay) {
        this.timeOfDay = timeOfDay;
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
        this.settlementManager.clear();
        this.coastalManager.clear();
        this.cropManager.clear();

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

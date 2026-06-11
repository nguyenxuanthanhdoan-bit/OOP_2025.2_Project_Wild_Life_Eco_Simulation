package model.world;

import model.entity.Entity;
import model.entity.Structure;
import core.GameConfig;
import model.living_beings.Animal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import core.Vector2;

/**
 * Lớp quản lý toàn bộ thực thể và môi trường của thế giới.
 */
public class World {

    public enum Season {
        GROWING("Sinh Trưởng"), WINTER("Khắc Nghiệt");
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
    private Season currentSeason = Season.GROWING;
    private int winterDays = 0; // Đếm số ngày của mùa đông
    private float winterProgress = 0.0f; // 0.0 -> Sinh Trưởng, 1.0 -> Mùa đông hoàn toàn
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
    private byte[] snowActivationMap;
    private int snowCacheWidth;
    private int snowCacheHeight;
    private static final int NO_SNOW = 255;
    private static final int MAX_SNOW_ACTIVATION = 254;
    private static final float SNOW_THRESHOLD_START = 0.9f;
    private static final float SNOW_THRESHOLD_RANGE = 1.1f;
    private static final float SNOW_TRANSITION_WIDTH = 0.18f;
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
            
            if (currentSeason == Season.GROWING) {
                int animalCount = 0;
                for (Entity e : getEntities()) {
                    if (e instanceof model.living_beings.Animal && e.isAlive()) {
                        animalCount++;
                    }
                }
                if (animalCount >= core.GameConfig.getInstance().MAX_INITIAL_ANIMAL_COUNT * 0.90f) {
                    setSeason(Season.WINTER);
                    winterDays = 0;
                }
            } else if (currentSeason == Season.WINTER) {
                winterDays++;
                if (winterDays >= 2) { // Kéo dài 2 ngày
                    setSeason(Season.GROWING);
                }
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
        cropManager.update(deltaTime, this);

        fishPopulationManager.update(deltaTime);

        // Cập nhật thời gian trong ngày
        timeOfDay += deltaTime * timeScale;
        if (timeOfDay >= 24.0f) {
            timeOfDay -= 24.0f;
        }

        // Cập nhật tiến trình mùa đông
        float winterTransitionRate = 1.0f / GameConfig.getInstance().WINTER_TRANSITION_SECONDS;
        if (currentSeason == Season.WINTER) {
            if (winterProgress < 1.0f) {
                winterProgress += deltaTime * winterTransitionRate;
                if (winterProgress > 1.0f) winterProgress = 1.0f;
            }
        } else {
            if (winterProgress > 0.0f) {
                winterProgress -= deltaTime * winterTransitionRate;
                if (winterProgress < 0.0f) winterProgress = 0.0f;
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

    public int getAnimalCount() {
        int count = 0;
        for (Entity e : entities) {
            if (e instanceof model.living_beings.Animal && e.isAlive()) {
                count++;
            }
        }
        return count;
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

        // Chỉ BLOCK là vật cản vật lý. AVOID được PathNavigator xử lý bằng chi phí A*.
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

    private boolean isAnimalBlockedFromSettlement(model.living_beings.LivingBeing entity) {
        if (entity instanceof model.living_beings.Animal) {
            model.living_beings.AnimalProfile.SettlementPolicy policy =
                    ((model.living_beings.Animal) entity).getProfile().getSettlementPolicy();
            return policy == model.living_beings.AnimalProfile.SettlementPolicy.BLOCK;
        }
        return false;
    }

    private boolean isAnimalBlockedFromGarden(model.living_beings.LivingBeing entity) {
        if (entity instanceof model.living_beings.Animal) {
            return !((model.living_beings.Animal) entity).getProfile().canEnterGardens();
        }
        return false;
    }

    private boolean collidesWithSolidStructure(model.living_beings.LivingBeing entity, Vector2 pos) {
        if (entity == null || pos == null || spatialGrid == null) return false;

        float entityRadius = entity.getCollider() != null
                ? entity.getCollider().getRadius()
                : entity.getSize() * 0.35f;
        float searchRadius = entityRadius + 80.0f;

        return spatialGrid.hasEntityMatching(pos, searchRadius, other -> {
            if (other == entity || !(other instanceof Structure) || !other.isSolid() || !other.isAlive()) {
                return false;
            }
            float otherRadius = other.getCollider() != null
                    ? other.getCollider().getRadius()
                    : other.getSize() * 0.35f;
            return pos.distanceTo(other.getPosition()) < entityRadius + otherRadius;
        });
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
        rebuildSnowActivationMap();
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
        } else if (currentSeason == Season.GROWING) {
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
        this.currentSeason = Season.GROWING;
        this.winterProgress = 0.0f;
        this.currentWeather = Weather.SUNNY;
        this.weatherTimer = 0.0f;
    }

    public float getDayTimer() { return dayTimer; }
    public int getGameDay() { return gameDay; }
    public Season getCurrentSeason() { return currentSeason; }
    public float getWinterProgress() { return winterProgress; }
    public Weather getCurrentWeather() { return currentWeather; }

    // --- VALUE NOISE METHODS FOR SNOW ---
    private float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h ^ (h >> 16)) & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    private float smoothNoise(float x, float y) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        float fx = x - ix;
        float fy = y - iy;

        float h00 = hash(ix, iy);
        float h10 = hash(ix + 1, iy);
        float h01 = hash(ix, iy + 1);
        float h11 = hash(ix + 1, iy + 1);

        float sx = fx * fx * (3.0f - 2.0f * fx);
        float sy = fy * fy * (3.0f - 2.0f * fy);

        float top = h00 + sx * (h10 - h00);
        float bottom = h01 + sx * (h11 - h01);

        return top + sy * (bottom - top);
    }

    private float getSnowNoise(float worldX, float worldY) {
        return smoothNoise(worldX * 0.004f, worldY * 0.004f) * 0.7f
                + smoothNoise(worldX * 0.01f, worldY * 0.01f) * 0.3f;
    }

    private void rebuildSnowActivationMap() {
        snowActivationMap = null;
        snowCacheWidth = 0;
        snowCacheHeight = 0;
        if (gameMap == null) return;

        int samples = GameConfig.getInstance().SNOW_MASK_SAMPLES_PER_TILE;
        snowCacheWidth = gameMap.getCols() * samples;
        snowCacheHeight = gameMap.getRows() * samples;
        if (snowCacheWidth <= 0 || snowCacheHeight <= 0) return;

        snowActivationMap = new byte[snowCacheWidth * snowCacheHeight];
        Arrays.fill(snowActivationMap, (byte) NO_SNOW);

        float worldWidth = gameMap.getCols() * GameConfig.getInstance().TILE_SIZE;
        float worldHeight = gameMap.getRows() * GameConfig.getInstance().TILE_SIZE;
        for (int y = 0; y < snowCacheHeight; y++) {
            float worldY = (y + 0.5f) / snowCacheHeight * worldHeight;
            for (int x = 0; x < snowCacheWidth; x++) {
                float worldX = (x + 0.5f) / snowCacheWidth * worldWidth;
                if (!gameMap.isSnowCoverablePixel(worldX, worldY)) continue;

                float activationProgress = (SNOW_THRESHOLD_START - getSnowNoise(worldX, worldY))
                        / SNOW_THRESHOLD_RANGE;
                activationProgress = Math.max(0.0f, Math.min(1.0f, activationProgress));
                int encoded = Math.round(activationProgress * MAX_SNOW_ACTIVATION);
                snowActivationMap[y * snowCacheWidth + x] = (byte) encoded;
            }
        }
    }

    private float getSnowDensityFromActivation(int encodedActivation, float progress) {
        if (encodedActivation == NO_SNOW || progress <= 0.0f) return 0.0f;

        float activationProgress = encodedActivation / (float) MAX_SNOW_ACTIVATION;
        float density = (progress - activationProgress)
                * (SNOW_THRESHOLD_RANGE / SNOW_TRANSITION_WIDTH);
        return Math.max(0.0f, Math.min(1.0f, density));
    }

    /**
     * Lấy mật độ tuyết tại một vị trí, dùng chung cho hình ảnh và logic di chuyển.
     * @param pos Tọa độ cần kiểm tra
     * @return Giá trị từ 0.0 (không có tuyết) đến 1.0 (tuyết dày đặc)
     */
    public float getSnowDensity(Vector2 pos) {
        if (winterProgress <= 0.0f || pos == null || snowActivationMap == null
                || gameMap == null) {
            return 0.0f;
        }

        float worldWidth = gameMap.getCols() * GameConfig.getInstance().TILE_SIZE;
        float worldHeight = gameMap.getRows() * GameConfig.getInstance().TILE_SIZE;
        if (pos.x < 0.0f || pos.y < 0.0f || pos.x >= worldWidth || pos.y >= worldHeight) {
            return 0.0f;
        }

        int x = Math.min(snowCacheWidth - 1, (int) (pos.x / worldWidth * snowCacheWidth));
        int y = Math.min(snowCacheHeight - 1, (int) (pos.y / worldHeight * snowCacheHeight));
        int encodedActivation = Byte.toUnsignedInt(snowActivationMap[y * snowCacheWidth + x]);
        return getSnowDensityFromActivation(encodedActivation, winterProgress);
    }

    public int getSnowCacheWidth() {
        return snowCacheWidth;
    }

    public int getSnowCacheHeight() {
        return snowCacheHeight;
    }

    /**
     * Ghi ảnh tuyết của một nấc tiến trình vào buffer do renderer cung cấp.
     * Mảng activation đã tính sẵn nên thao tác này chỉ còn là phép tra cứu tuyến tính.
     */
    public void fillSnowCoverage(int progressBucket, int bucketCount, int[] targetPixels) {
        if (snowActivationMap == null || targetPixels == null
                || targetPixels.length < snowActivationMap.length || bucketCount <= 0) {
            return;
        }

        float progress = Math.max(0.0f,
                Math.min(1.0f, progressBucket / (float) bucketCount));
        int[] colorsByActivation = new int[NO_SNOW + 1];
        for (int encodedActivation = 0;
             encodedActivation <= MAX_SNOW_ACTIVATION;
             encodedActivation++) {
            float density = getSnowDensityFromActivation(encodedActivation, progress);
            if (density <= 0.0f) continue;

            int alpha = Math.min(245, Math.max(20, Math.round(density * 245.0f)));
            colorsByActivation[encodedActivation] = (alpha << 24) | 0x00FFFFFF;
        }

        for (int i = 0; i < snowActivationMap.length; i++) {
            int encodedActivation = Byte.toUnsignedInt(snowActivationMap[i]);
            targetPixels[i] = colorsByActivation[encodedActivation];
        }
    }

}

package model.world;

import core.GameConfig;
import core.Vector2;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;
import model.plants.FruitTree;
import model.plants.Grass;
import model.plants.Mushroom;
import model.living_beings.Animal;
import model.living_beings.Rabbit;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Human;
import model.living_beings.Hunter;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.living_beings.HumanRole;
import model.structures.Bush;
import model.structures.Boat;
import model.structures.DecorativeStructure;
import model.structures.FishingHut;
import model.structures.FoodStorage;
import model.structures.House;
import model.structures.Rock;
import model.structures.Well;
import model.structures.GardenBed;
import model.entity.Entity;
import model.entity.Structure;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.living_beings.FishFactory;
import model.world.WaterTile;

/**
 * Lớp chuyên tạo và phân bố các vùng sinh thái (Biomes).
 */
public class BiomeGenerator {
    private static final String VILLAGE_ASSET_DIR = "resources/assets/images/village";

    public enum ZoneType {
        FOREST_ZONE, GRASSLAND_ZONE, RANDOM_ZONE
    }

    public static void generateBiomes(World world, GameMap gameMap) {
        Random rand = new Random();
        List<MapPolygonObject> polygons = gameMap.getBiomePolygons();

        List<MapPolygonObject> plainPolygons = new ArrayList<>();
        List<MapPolygonObject> forestPolygons = new ArrayList<>();
        List<MapPolygonObject> villagePolygons = new ArrayList<>();

        if (polygons != null) {
            for (MapPolygonObject poly : polygons) {
                if ("PLAIN".equalsIgnoreCase(poly.type)) plainPolygons.add(poly);
                else if ("FOREST".equalsIgnoreCase(poly.type)) forestPolygons.add(poly);
                else if ("VILLAGE".equalsIgnoreCase(poly.type)) villagePolygons.add(poly);
            }
        }
        if (gameMap.getVillagePolygons() != null) {
            villagePolygons.addAll(gameMap.getVillagePolygons());
        }
        
        while (GameConfig.getInstance().ENABLE_FALLBACK_VILLAGES
                && villagePolygons.size() < 2) {
            villagePolygons.add(createFallbackVillage(world, gameMap, rand, false));
        }
        
        // Tạo fallbacks thành các Zone mặc định nếu map không định nghĩa sẵn
        if (plainPolygons.isEmpty()) plainPolygons = createFallbackZone(world, ZoneType.GRASSLAND_ZONE);
        if (forestPolygons.isEmpty()) forestPolygons = createFallbackZone(world, ZoneType.FOREST_ZONE);

        // Sinh Động vật
        spawnAnimals(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        // Hệ thống sinh thái cá giờ được quản lý động bởi FishPopulationManager trong World.java
        
        // Sinh Cây cỏ
        spawnVegetation(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        spawnStructures(world, gameMap, plainPolygons, forestPolygons, rand);

        // Sinh công trình trong Village object layer; nhận lại map polygon → Settlement
        java.util.Map<MapPolygonObject, Settlement> settlementMap =
                spawnVillageStructures(world, gameMap, villagePolygons, rand);

        // Hoàn tất toàn bộ structure trước khi chọn điểm spawn cho cư dân.
        spawnLanterns(world, gameMap, rand);
        spawnCoastal(world, gameMap, rand);

        // Sinh dân làng/thợ săn sau cùng để clearance thấy đủ mọi vật cản.
        spawnVillagePeople(world, gameMap, settlementMap, rand);
    }

    private static MapPolygonObject createFallbackVillage(World world, GameMap gameMap, Random rand, boolean preferSand) {
        MapPolygonObject poly = new MapPolygonObject();
        poly.type = "VILLAGE";
        java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
        
        float cx = rand.nextFloat() * world.getWidth();
        float cy = rand.nextFloat() * world.getHeight();
        
        // Tìm vị trí phù hợp
        int attempts = 0;
        while (attempts < 10000) {
            cx = 100 + rand.nextFloat() * (world.getWidth() - 200);
            cy = 100 + rand.nextFloat() * (world.getHeight() - 200);
            boolean isSand = gameMap.isSandTile(cx, cy);
            if ((preferSand && isSand) || (!preferSand && !isSand && !gameMap.isPositionInWater(cx, cy))) {
                break;
            }
            attempts++;
        }
        
        
        // Vẽ 1 hình vuông nhỏ làm village (nhỏ hơn để ôm sát bãi cát)
        float s = 150f;
        p.moveTo(cx - s, cy - s);
        p.lineTo(cx + s, cy - s);
        p.lineTo(cx + s, cy + s);
        p.lineTo(cx - s, cy + s);
        p.closePath();
        poly.polygonPath = p;
        return poly;
    }

    private static List<MapPolygonObject> createFallbackZone(World world, ZoneType type) {
        List<MapPolygonObject> list = new ArrayList<>();
        // Giả lập vùng bằng cách chia màn hình (trái: grass, phải: forest)
        MapPolygonObject poly = new MapPolygonObject();
        poly.type = type.name();
        
        java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
        if (type == ZoneType.GRASSLAND_ZONE) {
            p.moveTo(0, 0);
            p.lineTo((int) (world.getWidth() / 2), 0);
            p.lineTo((int) (world.getWidth() / 2), (int) world.getHeight());
            p.lineTo(0, (int) world.getHeight());
            p.closePath();
        } else {
            p.moveTo((int) (world.getWidth() / 2), 0);
            p.lineTo((int) world.getWidth(), 0);
            p.lineTo((int) world.getWidth(), (int) world.getHeight());
            p.lineTo((int) (world.getWidth() / 2), (int) world.getHeight());
            p.closePath();
        }
        poly.polygonPath = p;
        list.add(poly);
        return list;
    }

    // =========================================================
    // SPAWN VEN BIỂN (THUYỀN + NHÀ CHÀI)
    // =========================================================

    /**
     * Spawn thuyền trên mặt nước và nhà chài gần mép biển.
     * Đăng ký với CoastalManager để AI Human có thể tìm đến ban ngày.
     */
    private static void spawnCoastal(World world, GameMap gameMap, Random rand) {
        GameConfig config = GameConfig.getInstance();
        // Đảo thứ tự: sinh nhà chài trước để làm mốc cho thuyền
        spawnFishingHuts(world, gameMap, rand, config.FISHING_HUT_COUNT);
        spawnBoats(world, gameMap, rand, config.BOAT_COUNT);
    }

    private static void spawnFarmForVillage(World world, GameMap gameMap, model.world.Settlement settlement, Random rand) {
        if (settlement.getHouses().isEmpty()) return;
        GameConfig config = GameConfig.getInstance();
        float gSize = config.GARDEN_BED_SIZE;
        float margin = gSize * 0.5f + 5f;
        
        // Tạo một trang trại lớn (3 hàng x 4 cột = 12 chậu)
        int cols = 4;
        int rows = 3;
        
        float farmWidth = cols * gSize;
        float farmHeight = rows * gSize;

        // Tính bounding box của nhà trong làng
        float hMinX = Float.MAX_VALUE, hMaxX = -Float.MAX_VALUE;
        float hMinY = Float.MAX_VALUE, hMaxY = -Float.MAX_VALUE;
        for (House h : settlement.getHouses()) {
            if (h.getPosition().x < hMinX) hMinX = h.getPosition().x;
            if (h.getPosition().x > hMaxX) hMaxX = h.getPosition().x;
            if (h.getPosition().y < hMinY) hMinY = h.getPosition().y;
            if (h.getPosition().y > hMaxY) hMaxY = h.getPosition().y;
        }

        // Hàng rào làng có padding là 100f, nên ta đặt vườn cách 120f để nằm sát mép ngoài hàng rào làng
        float padding = 120f;
        core.Vector2[] testStarts = {
            new core.Vector2(hMinX, hMaxY + padding), // Dưới
            new core.Vector2(hMinX, hMinY - padding - farmHeight), // Trên
            new core.Vector2(hMaxX + padding, hMinY), // Phải
            new core.Vector2(hMinX - padding - farmWidth, hMinY) // Trái
        };

        // Thử tìm vị trí nằm sát ngay rìa làng
        for (core.Vector2 start : testStarts) {
            float startX = start.x;
            float startY = start.y;
            
            // Kiểm tra toàn bộ các ô trong grid xem có hợp lệ (không dính nước) không
            boolean valid = true;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    float px = startX + col * gSize + gSize / 2;
                    float py = startY + row * gSize + gSize / 2;
                    if (!world.isValidGroundSpawnPosition(px, py, margin)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) break;
            }
            
            if (valid) {
                // Đủ diện tích, tiến hành spawn các chậu cây
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        float px = startX + col * gSize + gSize / 2;
                        float py = startY + row * gSize + gSize / 2;
                        GardenBed bed = new GardenBed(new core.Vector2(px, py));
                        world.addEntity(bed);
                        world.getCropManager().addGardenBed(bed);
                        settlement.addGardenBed(bed);
                    }
                }
                
                break; // Chỉ cần 1 trang trại lớn cho mỗi làng là đủ
            }
        }
    }

    // Đã gỡ bỏ spawnVillageFences theo yêu cầu của người chơi

    /**
     * Spawn thuyền tại các ô nước ngẫu nhiên trên bản đồ.
     * Thuyền không cản đường — chỉ là vật trang trí trên nước.
     */
    private static void spawnBoats(World world, GameMap gameMap, Random rand, int count) {
        if (gameMap == null || count <= 0) return;
        GameConfig config = GameConfig.getInstance();
        CoastalManager coastal = world.getCoastalManager();

        List<FishingHut> huts = coastal.getFishingHuts();

        int spawned = 0;
        int attempts = 0;
        int maxAttempts = count * 80;

        while (spawned < count && attempts < maxAttempts) {
            attempts++;
            
            float x, y;
            if (!huts.isEmpty()) {
                // Chọn một nhà chài ngẫu nhiên và sinh thuyền quanh nó
                FishingHut hut = huts.get(rand.nextInt(huts.size()));
                float angle = rand.nextFloat() * (float)Math.PI * 2;
                float dist = 40f + rand.nextFloat() * 100f; // Thuyền nằm sát nhà chài hơn
                x = hut.getPosition().x + (float)Math.cos(angle) * dist;
                y = hut.getPosition().y + (float)Math.sin(angle) * dist;
            } else {
                x = rand.nextFloat() * world.getWidth();
                y = rand.nextFloat() * world.getHeight();
            }

            // Phải là ô nước
            if (!gameMap.isPositionInWater(x, y)) continue;

            // Phải gần bờ để người có thể leo lên (cách bờ < 64 pixel)
            boolean nearShore = !gameMap.isPositionInWater(x - 64, y) ||
                                !gameMap.isPositionInWater(x + 64, y) ||
                                !gameMap.isPositionInWater(x, y - 64) ||
                                !gameMap.isPositionInWater(x, y + 64);
            if (!nearShore) continue;

            Vector2 pos = new Vector2(x, y);

            // Kiểm tra khoảng cách tối thiểu với thuyền khác
            if (!isFarFromBoats(coastal, pos, config.BOAT_MIN_DISTANCE)) continue;

            Boat boat = new Boat(pos);
            world.addEntity(boat);
            coastal.addBoat(boat);
            spawned++;
        }
    }

    /**
     * Spawn nhà chài gần mép biển: nằm trên đất, trong khoảng cách
     * FISHING_HUT_SHORE_MAX_DIST tính từ mép nước.
     */
    private static void spawnFishingHuts(World world, GameMap gameMap, Random rand, int count) {
        if (gameMap == null || count <= 0) return;
        GameConfig config = GameConfig.getInstance();
        CoastalManager coastal = world.getCoastalManager();
        float shoreMaxDist = config.FISHING_HUT_SHORE_MAX_DIST;
        float minDist = config.FISHING_HUT_MIN_DISTANCE;

        int spawned = 0;
        int attempts = 0;
        int maxAttempts = count * 120;
        
        List<model.world.Settlement> settlements = world.getSettlementManager().getSettlements();

        while (spawned < count && attempts < maxAttempts) {
            attempts++;
            float x, y;
            if (settlements != null && !settlements.isEmpty()) {
                model.world.Settlement targetVillage = settlements.get(rand.nextInt(settlements.size()));
                float angle = rand.nextFloat() * (float)Math.PI * 2;
                float r = rand.nextFloat() * 800f; // Trong bán kính 800px quanh làng
                x = targetVillage.getCenter().x + (float)Math.cos(angle) * r;
                y = targetVillage.getCenter().y + (float)Math.sin(angle) * r;
            } else {
                x = rand.nextFloat() * world.getWidth();
                y = rand.nextFloat() * world.getHeight();
            }

            // Phải là đất hợp lệ (không nước)
            if (!gameMap.isValidGroundSpawnPosition(x, y, config.GROUND_SPAWN_MARGIN)) continue;

            // Phải đủ gần mép nước
            if (!isNearWater(gameMap, x, y, shoreMaxDist)) continue;

            Vector2 pos = new Vector2(x, y);

            // Khoảng cách tối thiểu với nhà chài khác
            if (!isFarFromFishingHuts(coastal, pos, minDist)) continue;

            // Không va chạm với các structure khác
            if (!isFarFromExistingStructures(world, pos, minDist * 0.5f)) continue;

            FishingHut hut = new FishingHut(pos);
            world.addEntity(hut);
            coastal.addFishingHut(hut);
            spawned++;
        }
    }

    /**
     * Kiểm tra vị trí có nằm gần mép nước trong khoảng maxDist không.
     * Quét theo 8 hướng với bước stepSize để tìm ô nước gần nhất.
     */
    private static boolean isNearWater(GameMap gameMap, float x, float y, float maxDist) {
        int steps = (int) (maxDist / 32) + 1;
        float step = maxDist / steps;
        for (int i = 1; i <= steps; i++) {
            float r = step * i;
            // Quét 8 hướng
            if (gameMap.isPositionInWater(x + r, y)) return true;
            if (gameMap.isPositionInWater(x - r, y)) return true;
            if (gameMap.isPositionInWater(x, y + r)) return true;
            if (gameMap.isPositionInWater(x, y - r)) return true;
            if (gameMap.isPositionInWater(x + r, y + r)) return true;
            if (gameMap.isPositionInWater(x - r, y - r)) return true;
            if (gameMap.isPositionInWater(x + r, y - r)) return true;
            if (gameMap.isPositionInWater(x - r, y + r)) return true;
        }
        return false;
    }

    private static boolean isFarFromBoats(CoastalManager coastal, Vector2 pos, float minDist) {
        for (Boat b : coastal.getBoats()) {
            if (pos.distanceTo(b.getPosition()) < minDist) return false;
        }
        return true;
    }

    private static boolean isFarFromFishingHuts(CoastalManager coastal, Vector2 pos, float minDist) {
        for (FishingHut h : coastal.getFishingHuts()) {
            if (pos.distanceTo(h.getPosition()) < minDist) return false;
        }
        return true;
    }

    // =========================================================
    // SPAWN ĐỘNG VẬT
    // =========================================================

    private static void spawnAnimals(World world, GameMap gameMap, List<MapPolygonObject> plain,
                                     List<MapPolygonObject> forest, List<MapPolygonObject> excludedZones, Random rand) {
        spawnSpecies(world, gameMap, plain, excludedZones, 50, 4, rand, (pos, index) -> new Rabbit(pos));
        spawnSpecies(world, gameMap, forest, excludedZones, 18, 3, rand, (pos, index) -> new Rabbit(pos));

        spawnSpecies(world, gameMap, plain, excludedZones, 32, 3, rand, (pos, index) -> new Deer(pos, 1 + index / 16));
        spawnSpecies(world, gameMap, forest, excludedZones, 22, 4, rand, (pos, index) -> new Deer(pos, 3 + index / 12));

        spawnSpecies(world, gameMap, plain, excludedZones, 5, 1, rand, (pos, index) -> new Elephant(pos, 1));
        spawnSpecies(world, gameMap, forest, excludedZones, 12, 2, rand, (pos, index) -> new Elephant(pos, 2 + index / 6));

        spawnSpecies(world, gameMap, plain, excludedZones, 2, 0, rand, (pos, index) -> new Tiger(pos));
        spawnSpecies(world, gameMap, forest, excludedZones, 12, 2, rand, (pos, index) -> new Tiger(pos));

        spawnSpecies(world, gameMap, plain, excludedZones, 10, 1, rand, (pos, index) -> new Wolf(pos));
        spawnSpecies(world, gameMap, forest, excludedZones, 18, 3, rand, (pos, index) -> new Wolf(pos));

        int foxCountToSpawn = 5 + rand.nextInt(4); // 5 đến 8 con
        int foxesSpawned = 0;
        int maxAttempts = 1500;
        while (foxesSpawned < foxCountToSpawn && maxAttempts > 0) {
            maxAttempts--;
            float x = rand.nextFloat() * world.getWidth();
            float y = rand.nextFloat() * world.getHeight();
            if (gameMap == null || !gameMap.isSandTile(x, y)) continue;
            if (isInsideAnyPolygon(excludedZones, x, y)) continue;

            Vector2 pos = new Vector2(x, y);
            model.living_beings.Fox fox = new model.living_beings.Fox(pos);
            if (!world.isValidPositionFor(fox, pos)
                    || !hasVillageResidentClearance(world, fox, pos, 1.0f)) {
                continue;
            }

            addSpawnedAnimal(world, fox, rand);
            foxesSpawned++;
        }

        spawnMapWideWildlife(world, gameMap, excludedZones, rand, GameConfig.getInstance().MAX_INITIAL_ANIMAL_COUNT);
    }

    private static void spawnSpecies(World world, GameMap gameMap, List<MapPolygonObject> zones,
                                     List<MapPolygonObject> excludedZones,
                                     int totalCount, int minPerZone, Random rand, AnimalFactory factory) {
        if (zones == null || zones.isEmpty() || totalCount <= 0) return;

        int spawned = 0;
        int speciesIndex = 0;
        int guaranteedPerZone = Math.min(minPerZone, Math.max(0, totalCount / zones.size()));

        for (MapPolygonObject zone : zones) {
            for (int i = 0; i < guaranteedPerZone && spawned < totalCount; i++) {
                Vector2 pos = getRandomPointInPolygon(zone, gameMap, rand, excludedZones);
                if (pos != null) {
                    addSpawnedAnimal(world, factory.create(pos, speciesIndex++), rand);
                    spawned++;
                }
            }
        }

        while (spawned < totalCount) {
            Vector2 pos = getRandomPointInPolygons(zones, gameMap, rand, excludedZones);
            if (pos == null) break;
            addSpawnedAnimal(world, factory.create(pos, speciesIndex++), rand);
            spawned++;
        }
    }

    private static void addSpawnedAnimal(World world, Animal animal, Random rand) {
        GameConfig config = GameConfig.getInstance();
        double ageRatio = config.INITIAL_SPAWN_MIN_AGE_RATIO
                + rand.nextDouble() * (config.INITIAL_SPAWN_MAX_AGE_RATIO - config.INITIAL_SPAWN_MIN_AGE_RATIO);
        animal.setAge(animal.getMaxAge() * ageRatio);
        animal.setAdult(true);
        world.addEntity(animal);
    }

    private static void spawnMapWideWildlife(World world, GameMap gameMap, List<MapPolygonObject> excludedZones,
                                             Random rand, int targetAnimalCount) {
        GameConfig config = GameConfig.getInstance();
        int attempts = 0;
        int maxAttempts = targetAnimalCount * config.SUPPLEMENTAL_SPAWN_ATTEMPT_MULTIPLIER;
        while (countAnimals(world) < targetAnimalCount && attempts < maxAttempts) {
            attempts++;
            Vector2 pos = getRandomGroundPoint(world, gameMap, excludedZones, rand);
            if (pos == null) break;
            Animal animal = createSupplementalAnimal(pos, rand);
            if (animal != null) {
                addSpawnedAnimal(world, animal, rand);
            }
        }
    }

    private static Animal createSupplementalAnimal(Vector2 pos, Random rand) {
        float roll = rand.nextFloat();
        if (roll < 0.36f) return EntityFactory.createAnimal("Thỏ", pos, 0, rand);
        if (roll < 0.68f) return new Deer(pos, 10 + rand.nextInt(4));
        if (roll < 0.84f) return EntityFactory.createAnimal("Sói", pos, 0, rand);
        if (roll < 0.94f) return new Elephant(pos, 20 + rand.nextInt(3));
        return EntityFactory.createAnimal("Hổ", pos, 0, rand);
    }

    private static int countAnimals(World world) {
        int count = 0;
        for (model.entity.Entity entity : world.getEntities()) {
            if (entity instanceof Animal && entity.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private static void spawnVegetation(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, List<MapPolygonObject> village, Random rand) {
        GameConfig config = GameConfig.getInstance();

        // Grass (Rất nhiều ở Grassland)
        for (int i = 0; i < config.INITIAL_GRASS_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.GRASS_PLAIN_SPAWN_CHANCE;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Grass(pos));
        }

        // Mushroom (Rất nhiều ở Forest)
        for (int i = 0; i < config.INITIAL_MUSHROOM_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.MUSHROOM_PLAIN_SPAWN_CHANCE;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Mushroom(pos));
        }

        // Cây (Rất nhiều ở Forest - tạo thành rừng rậm)
        int[] normalTrees = {1, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        
        // Đồng cỏ: Cây mọc thưa
        for (int i = 0; i < config.INITIAL_PLAIN_TREE_COUNT; i++) {
            Vector2 pos = getRandomPointInPolygons(plain, gameMap, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }

        // Rừng: Mọc thành cụm rậm rạp
        for (int i = 0; i < config.INITIAL_FOREST_TREE_COUNT; i++) {
            Vector2 pos = getRandomPointInPolygons(forest, gameMap, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }

        // --- MỚI: Sinh thực vật sa mạc ---
        int desertVegetationSpawned = 0;
        int maxAttempts = 1500;
        while (desertVegetationSpawned < 40 && maxAttempts > 0) {
            maxAttempts--;
            float x = rand.nextFloat() * world.getWidth();
            float y = rand.nextFloat() * world.getHeight();
            if (gameMap != null && gameMap.isSandTile(x, y)) {
                Vector2 pos = new Vector2(x, y);
                if (rand.nextFloat() < 0.6f) { // 60% xương rồng
                    world.addEntity(new model.plants.Cactus(pos));
                } else { // 40% cỏ khô
                    world.addEntity(new model.plants.Straw(pos));
                }
                desertVegetationSpawned++;
            }
        }
    }

    private static void spawnStructures(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, Random rand) {
        GameConfig config = GameConfig.getInstance();

        // Bush (Nhiều ở Forest để động vật nhỏ trốn)
        for (int i = 0; i < config.INITIAL_BUSH_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.BUSH_PLAIN_SPAWN_CHANCE;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Bush(pos));
        }

        // Rock (Hỗn hợp)
        for (int i = 0; i < config.INITIAL_ROCK_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.ROCK_PLAIN_SPAWN_CHANCE;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Rock(pos));
        }
    }

    private static java.util.Map<MapPolygonObject, Settlement> spawnVillageStructures(
            World world, GameMap gameMap, List<MapPolygonObject> villages, Random rand) {
        java.util.Map<MapPolygonObject, Settlement> result = new java.util.LinkedHashMap<>();
        if (villages == null || villages.isEmpty()) return result;
        GameConfig config = GameConfig.getInstance();
        int houseVariants = countVillageAssets("house_");
        int wellVariants = countVillageAssets("well_");
        List<String> decorativeVariants = listVillageAssets("decorative_");
        List<String> marketVariants = listVillageAssets("market");

        for (MapPolygonObject village : villages) {
            Vector2 clusterCenter = findVillageClusterCenter(village, rand);
            int houseCount = config.MIN_HOUSES_PER_VILLAGE
                    + rand.nextInt(config.MAX_HOUSES_PER_VILLAGE - config.MIN_HOUSES_PER_VILLAGE + 1);

            int entityCountBefore = world.getEntities().size();

            spawnVillageGroup(world, gameMap, village, clusterCenter, houseCount, rand,
                    (pos, index) -> new House(pos, 1 + index % houseVariants, config.HOUSE_CAPACITY));
            spawnVillageGroup(world, gameMap, village, clusterCenter, config.WELLS_PER_VILLAGE, rand,
                    (pos, index) -> new Well(pos, 1 + index % wellVariants));
            spawnVillageGroup(world, gameMap, village, clusterCenter, config.FOOD_STORAGES_PER_VILLAGE, rand,
                    (pos, index) -> new FoodStorage(pos));
            spawnVillageGroup(world, gameMap, village, clusterCenter, config.DECORATIONS_PER_VILLAGE, rand,
                    (pos, index) -> new DecorativeStructure(pos,
                            selectDecorativeVariant(index, decorativeVariants, marketVariants)));

            if (clusterCenter != null) {
                float safeRadius = getVillageHomeRadius(village) + config.SETTLEMENT_SAFE_RADIUS_PADDING;
                Settlement settlement = new Settlement(clusterCenter, safeRadius);

                List<model.entity.Entity> allEntities = world.getEntities();
                for (int i = entityCountBefore; i < allEntities.size(); i++) {
                    model.entity.Entity e = allEntities.get(i);
                    if (e instanceof House) settlement.addHouse((House) e);
                    else if (e instanceof Well) settlement.addWell((Well) e);
                    else if (e instanceof FoodStorage) settlement.addFoodStorage((FoodStorage) e);
                }

                world.getSettlementManager().addSettlement(settlement);
                spawnFarmForVillage(world, gameMap, settlement, rand);

                // Trả về ánh xạ polygon → Settlement để spawnVillagePeople dùng đúng Settlement
                result.put(village, settlement);
            }
        }
        return result;
    }

    private static void spawnVillagePeople(World world, GameMap gameMap,
            java.util.Map<MapPolygonObject, Settlement> settlementMap, Random rand) {
        if (settlementMap == null || settlementMap.isEmpty()) return;
        GameConfig config = GameConfig.getInstance();

        for (java.util.Map.Entry<MapPolygonObject, Settlement> entry : settlementMap.entrySet()) {
            MapPolygonObject village = entry.getKey();
            Settlement settlement    = entry.getValue();
            // Dùng đúng tâm/bán kính của Settlement đã được tạo từ spawnVillageStructures
            Vector2 clusterCenter = settlement.getCenter();
            float homeRadius      = settlement.getSafeRadius();

            int villagerCount = config.VILLAGERS_PER_VILLAGE;
            int maleCount = villagerCount >= 2 ? villagerCount / 2 : 0;
            int femaleCount = villagerCount - maleCount;

            // Nhóm 1: Male Villager
            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, maleCount, rand,
                    (pos, index) -> createVillageHuman(pos, Human.Variant.MALE,
                            HumanRole.VILLAGER, settlement, clusterCenter, homeRadius));

            // Nhóm 2: Female Villager
            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, femaleCount, rand,
                    (pos, index) -> createVillageHuman(pos, Human.Variant.FEMALE,
                            HumanRole.VILLAGER, settlement, clusterCenter, homeRadius));

            // Nhóm 3: Hunter
            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, config.HUNTERS_PER_VILLAGE, rand,
                    (pos, index) -> {
                        Hunter hunter = new Hunter(pos, clusterCenter, homeRadius);
                        hunter.setHomeSettlement(settlement);
                        return hunter;
                    });

            // Nhóm 4: Fisherman (variant ngẫu nhiên, nghề nghiệp rõ ràng)
            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, config.FISHERMEN_PER_VILLAGE, rand,
                    (pos, index) -> {
                        Human.Variant v = rand.nextBoolean() ? Human.Variant.MALE : Human.Variant.FEMALE;
                        return createVillageHuman(pos, v, HumanRole.FISHERMAN,
                                settlement, clusterCenter, homeRadius);
                    });
        }
    }

    private static Human createVillageHuman(Vector2 pos, Human.Variant variant, HumanRole role,
                                            Settlement settlement, Vector2 center, float radius) {
        Human human = new Human(pos, variant, role, center, radius);
        human.setHomeSettlement(settlement);
        return human;
    }

    private static void spawnVillageGroup(World world, GameMap gameMap, MapPolygonObject village,
                                          Vector2 clusterCenter, int count, Random rand, StructureFactory factory) {
        for (int i = 0; i < count; i++) {
            Vector2 pos = getRandomVillagePoint(world, gameMap, village, clusterCenter, rand);
            if (pos != null) {
                world.addEntity(factory.create(pos, i));
            }
        }
    }

    private static void spawnVillageAnimalGroup(World world, GameMap gameMap, MapPolygonObject village,
                                                Vector2 clusterCenter, int count, Random rand, AnimalFactory factory) {
        if (count <= 0) return;
        List<Vector2> candidates = createVillageResidentCandidates(gameMap, village, clusterCenter, rand);

        for (int i = 0; i < count; i++) {
            Collections.shuffle(candidates, rand);
            for (Vector2 candidate : candidates) {
                Vector2 pos = candidate.copy();
                Animal animal = factory.create(pos, i);
                if (!isValidVillageResidentSpawn(world, gameMap, village, animal, pos)) continue;
                addSpawnedAnimal(world, animal, rand);
                break;
            }
        }
    }

    private static List<Vector2> createVillageResidentCandidates(GameMap gameMap,
            MapPolygonObject village, Vector2 clusterCenter, Random rand) {
        List<Vector2> candidates = new ArrayList<>();
        if (village == null || village.polygonPath == null) return candidates;

        Rectangle2D bounds = village.polygonPath.getBounds2D();
        float step = Math.max(20.0f, GameConfig.getInstance().TILE_SIZE * 0.75f);
        for (float y = (float) bounds.getMinY() + step / 2; y < bounds.getMaxY(); y += step) {
            for (float x = (float) bounds.getMinX() + step / 2; x < bounds.getMaxX(); x += step) {
                if (!village.polygonPath.contains(x, y)) continue;
                if (gameMap != null && gameMap.isPositionInWater(x, y)
                        && !gameMap.isBridgeTile(x, y)) {
                    continue;
                }
                candidates.add(new Vector2(x, y));
            }
        }

        for (int i = 0; i < 300; i++) {
            Vector2 candidate = i < 180
                    ? randomClusteredPoint(village, clusterCenter, rand, 1.0f)
                    : getRandomPointInsidePolygon(village, rand);
            if (candidate != null
                    && (gameMap == null || !gameMap.isPositionInWater(candidate.x, candidate.y)
                    || gameMap.isBridgeTile(candidate.x, candidate.y))) {
                candidates.add(candidate);
            }
        }
        Collections.shuffle(candidates, rand);
        return candidates;
    }

    private static boolean isValidVillageResidentSpawn(World world, GameMap gameMap, MapPolygonObject village,
                                                       Animal animal, Vector2 pos) {
        if (world == null || village == null || village.polygonPath == null || animal == null || pos == null) {
            return false;
        }
        if (!village.polygonPath.contains(pos.x, pos.y)) return false;
        
        // Tuyệt đối không spawn trên mặt nước
        if (gameMap != null && gameMap.isPositionInWater(pos.x, pos.y) && !gameMap.isBridgeTile(pos.x, pos.y)) {
            return false;
        }
        if (!world.isValidPositionFor(animal, pos)) return false;

        return hasVillageResidentClearance(world, animal, pos, 0.7f);
    }

    private static boolean hasVillageResidentClearance(World world, Animal animal, Vector2 pos, float strictness) {
        if (world.getSpatialGrid() == null) return true;

        float scanRange = 150.0f;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(pos, scanRange);
        for (Entity entity : nearby) {
            if (entity == animal || !entity.isAlive()) continue;

            if (entity instanceof Structure && entity.isSolid()) {
                float minDistance = animal.getSize() * 0.55f + entity.getSize() * 0.72f + 12.0f;
                if (pos.distanceTo(entity.getPosition()) < minDistance) {
                    return false;
                }
            } else if (entity instanceof Animal) {
                float minDistance = (animal.getSize() + entity.getSize()) * 0.55f;
                if (pos.distanceTo(entity.getPosition()) < minDistance * strictness) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int countVillageAssets(String prefix) {
        return Math.max(1, listVillageAssets(prefix).size());
    }

    private static List<String> listVillageAssets(String prefix) {
        List<String> variants = new ArrayList<>();
        File dir = new File(VILLAGE_ASSET_DIR);
        File[] files = dir.listFiles((parent, name) ->
                name.toLowerCase().startsWith(prefix.toLowerCase()) &&
                name.toLowerCase().endsWith(".png"));
        if (files == null) return variants;

        for (File file : files) {
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                variants.add(name.substring(0, dotIndex).toLowerCase());
            }
        }
        variants.sort(String::compareTo);
        return variants;
    }

    private static String selectDecorativeVariant(int index, List<String> decorativeVariants, List<String> marketVariants) {
        if (!marketVariants.isEmpty() && index < marketVariants.size()) {
            return marketVariants.get(index);
        }
        if (!decorativeVariants.isEmpty()) {
            int decorativeIndex = Math.max(0, index - marketVariants.size());
            return decorativeVariants.get(decorativeIndex % decorativeVariants.size());
        }
        if (!marketVariants.isEmpty()) {
            return marketVariants.get(index % marketVariants.size());
        }
        return "decorative_1";
    }

    private static void spawnLanterns(World world, GameMap gameMap, Random rand) {
        int minBridgeX = Integer.MAX_VALUE;
        int maxBridgeX = Integer.MIN_VALUE;
        int minBridgeY = Integer.MAX_VALUE;
        int maxBridgeY = Integer.MIN_VALUE;
        boolean bridgeFound = false;

        for (int c = 0; c < gameMap.getCols(); c++) {
            for (int r = 0; r < gameMap.getRows(); r++) {
                if (gameMap.isBridgeTile(c * 32 + 16, r * 32 + 16)) {
                    bridgeFound = true;
                    if (c * 32 < minBridgeX) minBridgeX = c * 32;
                    if (c * 32 > maxBridgeX) maxBridgeX = c * 32;
                    if (r * 32 < minBridgeY) minBridgeY = r * 32;
                    if (r * 32 > maxBridgeY) maxBridgeY = r * 32;
                }
            }
        }

        if (bridgeFound) {
            float width = maxBridgeX - minBridgeX;
            float height = maxBridgeY - minBridgeY;
            if (width > height) {
                world.addEntity(new model.structures.Lantern(new Vector2(minBridgeX - 32, (minBridgeY + maxBridgeY) / 2f + 16), "lantern"));
                world.addEntity(new model.structures.Lantern(new Vector2(maxBridgeX + 64, (minBridgeY + maxBridgeY) / 2f + 16), "lantern"));
            } else {
                world.addEntity(new model.structures.Lantern(new Vector2((minBridgeX + maxBridgeX) / 2f + 16, minBridgeY - 32), "lantern"));
                world.addEntity(new model.structures.Lantern(new Vector2((minBridgeX + maxBridgeX) / 2f + 16, maxBridgeY + 64), "lantern"));
            }
        }

        int lanternsSpawned = 0;
        int attempts = 0;
        while (lanternsSpawned < 30 && attempts < 300) {
            attempts++;
            Vector2 pos = getRandomGroundPoint(world, gameMap, rand);
            if (pos != null) {
                boolean collision = false;
                for (model.entity.Entity e : world.getEntities()) {
                    if (e.isSolid() && e.getPosition().distanceTo(pos) < e.getSize() / 2f + 16f) {
                        collision = true;
                        break;
                    }
                }
                if (!collision) {
                    world.addEntity(new model.structures.Lantern(pos, "lantern"));
                    lanternsSpawned++;
                }
            }
        }
    }

    private static Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, GameMap gameMap, Random rand) {
        return getRandomPointInPolygons(polys, gameMap, rand, null);
    }

    private static Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, GameMap gameMap, Random rand,
                                                    List<MapPolygonObject> excludedPolys) {
        if (polys == null || polys.isEmpty()) return null;
        for (int attempt = 0; attempt < polys.size() * 2; attempt++) {
            Vector2 pos = getRandomPointInPolygon(selectPolygonByArea(polys, rand), gameMap, rand, excludedPolys);
            if (pos != null) return pos;
        }
        return null;
    }

    private static Vector2 getRandomPointInPolygon(MapPolygonObject selectedPoly, GameMap gameMap, Random rand) {
        return getRandomPointInPolygon(selectedPoly, gameMap, rand, null);
    }

    private static Vector2 getRandomPointInPolygon(MapPolygonObject selectedPoly, GameMap gameMap, Random rand,
                                                   List<MapPolygonObject> excludedPolys) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return null;

        GameConfig config = GameConfig.getInstance();
        for (int attempt = 0; attempt < config.SPAWN_ATTEMPTS_PER_POINT; attempt++) {
            float x = (float) (bounds.getX() + rand.nextDouble() * bounds.getWidth());
            float y = (float) (bounds.getY() + rand.nextDouble() * bounds.getHeight());
            if (selectedPoly.polygonPath.contains(x, y)) {
                if (isInsideAnyPolygon(excludedPolys, x, y)) continue;
                if (gameMap != null) {
                    if (gameMap.isValidGroundSpawnPosition(x, y, config.GROUND_SPAWN_MARGIN)) {
                        return new Vector2(x, y);
                    }
                } else {
                    return new Vector2(x, y);
                }
            }
        }
        return null;
    }

    private static Vector2 getRandomGroundPoint(World world, GameMap gameMap, Random rand) {
        return getRandomGroundPoint(world, gameMap, null, rand);
    }

    private static Vector2 getRandomGroundPoint(World world, GameMap gameMap,
                                                List<MapPolygonObject> excludedPolys, Random rand) {
        if (world == null) return null;
        GameConfig config = GameConfig.getInstance();
        for (int attempt = 0; attempt < config.SPAWN_ATTEMPTS_PER_POINT; attempt++) {
            float x = rand.nextFloat() * world.getWidth();
            float y = rand.nextFloat() * world.getHeight();
            if (isInsideAnyPolygon(excludedPolys, x, y)) continue;
            if (gameMap == null || gameMap.isValidGroundSpawnPosition(x, y, config.GROUND_SPAWN_MARGIN)) {
                return new Vector2(x, y);
            }
        }
        return null;
    }

    private static Vector2 findVillageClusterCenter(MapPolygonObject selectedPoly, Random rand) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        Vector2 center = new Vector2((float) bounds.getCenterX(), (float) bounds.getCenterY());
        if (selectedPoly.polygonPath.contains(center.x, center.y)) {
            return center;
        }
        return getRandomPointInsidePolygon(selectedPoly, rand);
    }

    private static float getVillageHomeRadius(MapPolygonObject selectedPoly) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) {
            return GameConfig.getInstance().VILLAGE_STRUCTURE_CLUSTER_RADIUS;
        }
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        float radius = (float) (Math.max(bounds.getWidth(), bounds.getHeight()) / 2.0);
        return radius + GameConfig.getInstance().VILLAGE_HOME_RADIUS_PADDING;
    }

    private static Vector2 getRandomPointInsidePolygon(MapPolygonObject selectedPoly, Random rand) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        GameConfig config = GameConfig.getInstance();
        for (int attempt = 0; attempt < 1000; attempt++) {
            float x = (float) (bounds.getX() + rand.nextDouble() * bounds.getWidth());
            float y = (float) (bounds.getY() + rand.nextDouble() * bounds.getHeight());
            if (selectedPoly.polygonPath.contains(x, y)) {
                return new Vector2(x, y);
            }
        }
        return null;
    }

    private static Vector2 getRandomVillagePoint(World world, GameMap gameMap, MapPolygonObject selectedPoly,
                                                 Vector2 clusterCenter, Random rand) {
        if (world == null || selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return null;

        GameConfig config = GameConfig.getInstance();
        float[] minDistances = {
                config.VILLAGE_STRUCTURE_MIN_DISTANCE,
                config.VILLAGE_STRUCTURE_MIN_DISTANCE * 0.9f,
                config.VILLAGE_STRUCTURE_MIN_DISTANCE * 0.8f
        };

        for (float minDistance : minDistances) {
            for (int attempt = 0; attempt < 1000; attempt++) {
                // Giảm dần bán kính cluster nếu fail nhiều lần để ép nhà vào bãi đất nhỏ
                float clusterRadiusScale = attempt < 500 ? 1.0f : (attempt < 800 ? 0.5f : 0.1f);
                Vector2 candidate = randomClusteredPoint(selectedPoly, clusterCenter, rand, clusterRadiusScale);
                if (candidate == null) continue;
                float x = candidate.x;
                float y = candidate.y;
                if (!selectedPoly.polygonPath.contains(x, y)) continue;
                if (gameMap != null && gameMap.isPositionInWater(x, y) && !gameMap.isBridgeTile(x, y)) continue;
                if (!isFarFromExistingStructures(world, candidate, minDistance)) continue;
                return candidate;
            }
        }

        // Không tìm được vị trí phù hợp, chấp nhận bỏ qua thay vì force spawn đè lên nhau
        return null;
    }

    private static Vector2 randomClusteredPoint(MapPolygonObject selectedPoly, Vector2 clusterCenter, Random rand, float radiusScale) {
        if (clusterCenter == null) return getRandomPointInsidePolygon(selectedPoly, rand);
        GameConfig config = GameConfig.getInstance();
        double angle = rand.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(rand.nextDouble()) * config.VILLAGE_STRUCTURE_CLUSTER_RADIUS * radiusScale;
        return new Vector2(
                (float) (clusterCenter.x + Math.cos(angle) * radius),
                (float) (clusterCenter.y + Math.sin(angle) * radius)
        );
    }

    private static boolean isFarFromExistingStructures(World world, Vector2 pos, float minDistance) {
        if (world == null || pos == null || world.getSpatialGrid() == null) return true;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(pos, minDistance);
        for (Entity entity : nearby) {
            if (entity instanceof Structure && entity.isAlive()) {
                if (entity.getPosition().distanceTo(pos) < minDistance) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isInsideAnyPolygon(List<MapPolygonObject> polygons, float x, float y) {
        if (polygons == null || polygons.isEmpty()) return false;
        for (MapPolygonObject polygon : polygons) {
            if (polygon != null && polygon.polygonPath != null && polygon.polygonPath.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static MapPolygonObject selectPolygonByArea(List<MapPolygonObject> polys, Random rand) {
        double totalArea = 0;
        for (MapPolygonObject poly : polys) {
            if (poly == null || poly.polygonPath == null) continue;
            Rectangle2D bounds = poly.polygonPath.getBounds2D();
            totalArea += Math.max(0, bounds.getWidth() * bounds.getHeight());
        }

        if (totalArea <= 0) return polys.get(rand.nextInt(polys.size()));

        double pick = rand.nextDouble() * totalArea;
        for (MapPolygonObject poly : polys) {
            if (poly == null || poly.polygonPath == null) continue;
            Rectangle2D bounds = poly.polygonPath.getBounds2D();
            pick -= Math.max(0, bounds.getWidth() * bounds.getHeight());
            if (pick <= 0) return poly;
        }
        return polys.get(polys.size() - 1);
    }

    private interface AnimalFactory {
        Animal create(Vector2 position, int index);
    }

    private interface StructureFactory {
        Structure create(Vector2 position, int index);
    }
}

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
import model.structures.Bush;
import model.structures.DecorativeStructure;
import model.structures.FoodStorage;
import model.structures.House;
import model.structures.Rock;
import model.structures.Well;
import model.entity.Entity;
import model.entity.Structure;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
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
        
        // Tạo fallbacks thành các Zone mặc định nếu map không định nghĩa sẵn
        if (plainPolygons.isEmpty()) plainPolygons = createFallbackZone(world, ZoneType.GRASSLAND_ZONE);
        if (forestPolygons.isEmpty()) forestPolygons = createFallbackZone(world, ZoneType.FOREST_ZONE);

        // Sinh Động vật
        spawnAnimals(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        // Hệ thống sinh thái cá giờ được quản lý động bởi FishPopulationManager trong World.java
        
        // Sinh Cây cỏ
        spawnVegetation(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        spawnStructures(world, gameMap, plainPolygons, forestPolygons, rand);

        // Sinh công trình trong Village object layer
        spawnVillageStructures(world, gameMap, villagePolygons, rand);

        // Sinh dân làng/thợ săn trong Village object layer
        spawnVillagePeople(world, gameMap, villagePolygons, rand);
        spawnLanterns(world, gameMap, rand);
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

    private static void spawnVillageStructures(World world, GameMap gameMap, List<MapPolygonObject> villages, Random rand) {
        if (villages == null || villages.isEmpty()) return;
        GameConfig config = GameConfig.getInstance();
        int houseVariants = countVillageAssets("house_");
        int wellVariants = countVillageAssets("well_");
        List<String> decorativeVariants = listVillageAssets("decorative_");
        List<String> marketVariants = listVillageAssets("market");

        for (MapPolygonObject village : villages) {
            Vector2 clusterCenter = findVillageClusterCenter(village, rand);
            int houseCount = config.MIN_HOUSES_PER_VILLAGE
                    + rand.nextInt(config.MAX_HOUSES_PER_VILLAGE - config.MIN_HOUSES_PER_VILLAGE + 1);

            spawnVillageGroup(world, gameMap, village, clusterCenter, houseCount, rand,
                    (pos, index) -> new House(pos, 1 + index % houseVariants, config.HOUSE_CAPACITY));
            spawnVillageGroup(world, gameMap, village, clusterCenter, config.WELLS_PER_VILLAGE, rand,
                    (pos, index) -> new Well(pos, 1 + index % wellVariants));
            spawnVillageGroup(world, gameMap, village, clusterCenter, config.FOOD_STORAGES_PER_VILLAGE, rand,
                    (pos, index) -> new FoodStorage(pos));
            spawnVillageGroup(world, gameMap, village, clusterCenter, config.DECORATIONS_PER_VILLAGE, rand,
                    (pos, index) -> new DecorativeStructure(pos,
                            selectDecorativeVariant(index, decorativeVariants, marketVariants)));
        }
    }

    private static void spawnVillagePeople(World world, GameMap gameMap, List<MapPolygonObject> villages, Random rand) {
        if (villages == null || villages.isEmpty()) return;
        GameConfig config = GameConfig.getInstance();

        for (MapPolygonObject village : villages) {
            Vector2 clusterCenter = findVillageClusterCenter(village, rand);
            float homeRadius = getVillageHomeRadius(village);
            int humanCount = config.HUMANS_PER_VILLAGE;
            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, config.HUMANS_PER_VILLAGE, rand,
                    (pos, index) -> new Human(pos,
                            villageHumanVariant(index, humanCount),
                            clusterCenter,
                            homeRadius));
            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, config.HUNTERS_PER_VILLAGE, rand,
                    (pos, index) -> new Hunter(pos, clusterCenter, homeRadius));
        }
    }

    private static Human.Variant villageHumanVariant(int index, int totalCount) {
        if (totalCount <= 1) return Human.Variant.MALE;
        if (index == 0) return Human.Variant.MALE;
        if (index == 1) return Human.Variant.FEMALE;
        return index % 2 == 0 ? Human.Variant.MALE : Human.Variant.FEMALE;
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
        GameConfig config = GameConfig.getInstance();

        for (int i = 0; i < count; i++) {
            int maxAttempts = config.SPAWN_ATTEMPTS_PER_POINT * 8;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                Vector2 pos = randomVillageResidentPoint(village, clusterCenter, rand, attempt, maxAttempts);
                Animal animal = pos == null ? null : factory.create(pos, i);
                if (!isValidVillageResidentSpawn(world, gameMap, village, animal, pos, attempt, maxAttempts)) {
                    continue;
                }
                addSpawnedAnimal(world, animal, rand);
                break;
            }
        }
    }

    private static Vector2 randomVillageResidentPoint(MapPolygonObject village, Vector2 clusterCenter,
                                                      Random rand, int attempt, int maxAttempts) {
        if (attempt < maxAttempts / 2) {
            return randomClusteredPoint(village, clusterCenter, rand);
        }
        return getRandomPointInsidePolygon(village, rand);
    }

    private static boolean isValidVillageResidentSpawn(World world, GameMap gameMap, MapPolygonObject village,
                                                       Animal animal, Vector2 pos, int attempt, int maxAttempts) {
        if (world == null || village == null || village.polygonPath == null || animal == null || pos == null) {
            return false;
        }
        if (!village.polygonPath.contains(pos.x, pos.y)) return false;
        if (gameMap != null && gameMap.isPositionInWater(pos.x, pos.y) && !gameMap.isBridgeTile(pos.x, pos.y)) {
            return false;
        }
        if (!world.isValidPositionFor(animal, pos)) return false;

        float strictness = attempt < maxAttempts / 3 ? 1.0f : (attempt < maxAttempts * 2 / 3 ? 0.75f : 0.55f);
        return hasVillageResidentClearance(world, animal, pos, strictness);
    }

    private static boolean hasVillageResidentClearance(World world, Animal animal, Vector2 pos, float strictness) {
        if (world.getSpatialGrid() == null) return true;

        float scanRange = 150.0f;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(pos, scanRange);
        for (Entity entity : nearby) {
            if (entity == animal || !entity.isAlive()) continue;

            if (entity instanceof Structure && entity.isSolid()) {
                float minDistance = animal.getSize() * 0.55f + entity.getSize() * 0.72f + 12.0f;
                if (pos.distanceTo(entity.getPosition()) < minDistance * strictness) {
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
        for (int attempt = 0; attempt < config.SPAWN_ATTEMPTS_PER_POINT; attempt++) {
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
            for (int attempt = 0; attempt < config.SPAWN_ATTEMPTS_PER_POINT * 4; attempt++) {
                Vector2 candidate = randomClusteredPoint(selectedPoly, clusterCenter, rand);
                if (candidate == null) continue;
                float x = candidate.x;
                float y = candidate.y;
                if (!selectedPoly.polygonPath.contains(x, y)) continue;
                if (gameMap != null && gameMap.isPositionInWater(x, y) && !gameMap.isBridgeTile(x, y)) continue;
                if (!isFarFromExistingStructures(world, candidate, minDistance)) continue;
                return candidate;
            }
        }
        return null;
    }

    private static Vector2 randomClusteredPoint(MapPolygonObject selectedPoly, Vector2 clusterCenter, Random rand) {
        if (clusterCenter == null) return getRandomPointInsidePolygon(selectedPoly, rand);
        GameConfig config = GameConfig.getInstance();
        double angle = rand.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(rand.nextDouble()) * config.VILLAGE_STRUCTURE_CLUSTER_RADIUS;
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

package model.world;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Human;
import model.living_beings.HumanRole;
import model.living_beings.Hunter;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;
import model.structures.DecorativeStructure;
import model.structures.FoodStorage;
import model.structures.GardenBed;
import model.structures.House;
import model.structures.Well;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VillageSpawner {

    private static final String VILLAGE_ASSET_DIR = "resources/assets/images/village";

    public static java.util.Map<MapPolygonObject, Settlement> spawnVillageStructures(
            World world, GameMap gameMap, List<MapPolygonObject> villages, Random rand) {
        java.util.Map<MapPolygonObject, Settlement> result = new java.util.LinkedHashMap<>();
        if (villages == null || villages.isEmpty()) return result;
        GameConfig config = GameConfig.getInstance();
        int houseVariants = countVillageAssets("house_");
        int wellVariants = countVillageAssets("well_");
        List<String> decorativeVariants = listVillageAssets("decorative_");
        List<String> marketVariants = listVillageAssets("market");

        for (MapPolygonObject village : villages) {
            Vector2 clusterCenter = SpawnHelper.findVillageClusterCenter(village, rand);
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
            
            int lanternCount = 3 + rand.nextInt(3);
            spawnVillageGroup(world, gameMap, village, clusterCenter, lanternCount, rand,
                    (pos, index) -> new model.structures.Lantern(pos, rand.nextBoolean() ? "lantern_2" : "lantern_3"));

            if (clusterCenter != null) {
                float safeRadius = SpawnHelper.getVillageHomeRadius(village) + config.SETTLEMENT_SAFE_RADIUS_PADDING;
                Settlement settlement = new Settlement(clusterCenter, safeRadius);

                List<Entity> allEntities = world.getEntities();
                for (int i = entityCountBefore; i < allEntities.size(); i++) {
                    Entity e = allEntities.get(i);
                    if (e instanceof House) settlement.addHouse((House) e);
                    else if (e instanceof Well) settlement.addWell((Well) e);
                    else if (e instanceof FoodStorage) settlement.addFoodStorage((FoodStorage) e);
                }

                world.getSettlementManager().addSettlement(settlement);
                spawnFarmForVillage(world, gameMap, settlement, rand);

                result.put(village, settlement);
            }
        }
        return result;
    }

    public static void spawnVillagePeople(World world, GameMap gameMap,
            java.util.Map<MapPolygonObject, Settlement> settlementMap, Random rand) {
        if (settlementMap == null || settlementMap.isEmpty()) return;
        GameConfig config = GameConfig.getInstance();

        for (java.util.Map.Entry<MapPolygonObject, Settlement> entry : settlementMap.entrySet()) {
            MapPolygonObject village = entry.getKey();
            Settlement settlement    = entry.getValue();
            Vector2 clusterCenter = settlement.getCenter();
            float homeRadius      = settlement.getSafeRadius();

            int villagerCount = config.VILLAGERS_PER_VILLAGE;
            int maleCount = villagerCount >= 2 ? villagerCount / 2 : 0;
            int femaleCount = villagerCount - maleCount;

            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, maleCount, rand,
                    (pos, index) -> createVillageHuman(pos, Human.Variant.MALE,
                            HumanRole.VILLAGER, settlement, clusterCenter, homeRadius));

            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, femaleCount, rand,
                    (pos, index) -> createVillageHuman(pos, Human.Variant.FEMALE,
                            HumanRole.VILLAGER, settlement, clusterCenter, homeRadius));

            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, config.HUNTERS_PER_VILLAGE, rand,
                    (pos, index) -> {
                        Hunter hunter = new Hunter(pos, clusterCenter, homeRadius);
                        hunter.setHomeSettlement(settlement);
                        return hunter;
                    });

            spawnVillageAnimalGroup(world, gameMap, village, clusterCenter, config.FISHERMEN_PER_VILLAGE, rand,
                    (pos, index) -> {
                        Human.Variant v = rand.nextBoolean() ? Human.Variant.MALE : Human.Variant.FEMALE;
                        return createVillageHuman(pos, v, HumanRole.FISHERMAN,
                                settlement, clusterCenter, homeRadius);
                    });
        }
    }

    private static void spawnFarmForVillage(World world, GameMap gameMap, Settlement settlement, Random rand) {
        if (settlement.getHouses().isEmpty()) return;
        GameConfig config = GameConfig.getInstance();
        float gSize = config.GARDEN_BED_SIZE;
        float margin = gSize * 0.5f + 5f;
        
        int cols = 4;
        int rows = 3;
        
        float farmWidth = cols * gSize;
        float farmHeight = rows * gSize;

        float hMinX = Float.MAX_VALUE, hMaxX = -Float.MAX_VALUE;
        float hMinY = Float.MAX_VALUE, hMaxY = -Float.MAX_VALUE;
        for (House h : settlement.getHouses()) {
            if (h.getPosition().x < hMinX) hMinX = h.getPosition().x;
            if (h.getPosition().x > hMaxX) hMaxX = h.getPosition().x;
            if (h.getPosition().y < hMinY) hMinY = h.getPosition().y;
            if (h.getPosition().y > hMaxY) hMaxY = h.getPosition().y;
        }

        float padding = 120f;
        core.Vector2[] testStarts = {
            new core.Vector2(hMinX, hMaxY + padding),
            new core.Vector2(hMinX, hMinY - padding - farmHeight),
            new core.Vector2(hMaxX + padding, hMinY),
            new core.Vector2(hMinX - padding - farmWidth, hMinY)
        };

        for (core.Vector2 start : testStarts) {
            float startX = start.x;
            float startY = start.y;
            
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
                break;
            }
        }
    }

    private static Human createVillageHuman(Vector2 pos, Human.Variant variant, HumanRole role,
                                            Settlement settlement, Vector2 center, float radius) {
        Human human = new Human(pos, variant, role, center, radius);
        human.setHomeSettlement(settlement);
        return human;
    }

    private static void spawnVillageGroup(World world, GameMap gameMap, MapPolygonObject village,
                                          Vector2 clusterCenter, int count, Random rand, SpawnHelper.StructureFactory factory) {
        for (int i = 0; i < count; i++) {
            Vector2 pos = SpawnHelper.getRandomVillagePoint(world, gameMap, village, clusterCenter, rand);
            if (pos != null) {
                world.addEntity(factory.create(pos, i));
            }
        }
    }

    private static void spawnVillageAnimalGroup(World world, GameMap gameMap, MapPolygonObject village,
                                                Vector2 clusterCenter, int count, Random rand, SpawnHelper.AnimalFactory factory) {
        if (count <= 0) return;
        List<Vector2> candidates = createVillageResidentCandidates(gameMap, village, clusterCenter, rand);

        for (int i = 0; i < count; i++) {
            Collections.shuffle(candidates, rand);
            for (Vector2 candidate : candidates) {
                Vector2 pos = candidate.copy();
                Animal animal = factory.create(pos, i);
                if (!isValidVillageResidentSpawn(world, gameMap, village, animal, pos)) continue;
                AnimalSpawner.addSpawnedAnimal(world, animal, rand);
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
                    ? SpawnHelper.randomClusteredPoint(village, clusterCenter, rand, 1.0f)
                    : SpawnHelper.getRandomPointInsidePolygon(village, rand);
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
        
        if (gameMap != null && gameMap.isPositionInWater(pos.x, pos.y) && !gameMap.isBridgeTile(pos.x, pos.y)) {
            return false;
        }
        if (!world.isValidPositionFor(animal, pos)) return false;

        return SpawnHelper.hasVillageResidentClearance(world, animal, pos, 0.7f);
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
}

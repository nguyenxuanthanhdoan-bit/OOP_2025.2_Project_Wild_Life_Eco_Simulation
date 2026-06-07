package model.world;

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
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.structures.Bush;
import model.structures.Rock;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lớp chuyên tạo và phân bố các vùng sinh thái (Biomes).
 */
public class BiomeGenerator {
    private static final int INITIAL_GRASS_COUNT = 320;
    private static final float GRASS_PLAIN_SPAWN_CHANCE = 0.9f;
    private static final int MAX_INITIAL_ANIMAL_COUNT = 250;
    private static final int SPAWN_ATTEMPTS_PER_POINT = 180;

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
        
        // Tạo fallbacks thành các Zone mặc định nếu map không định nghĩa sẵn
        if (plainPolygons.isEmpty()) plainPolygons = createFallbackZone(world, ZoneType.GRASSLAND_ZONE);
        if (forestPolygons.isEmpty()) forestPolygons = createFallbackZone(world, ZoneType.FOREST_ZONE);

        // Sinh Động vật
        spawnAnimals(world, gameMap, plainPolygons, forestPolygons, rand);
        
        // Sinh Cây cỏ
        spawnVegetation(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        
        // Sinh Môi trường (Đá, Bụi)
        spawnStructures(world, gameMap, plainPolygons, forestPolygons, rand);
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

    private static void spawnAnimals(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, Random rand) {
        spawnSpecies(world, gameMap, plain, 50, 4, rand, (pos, index) -> new Rabbit(pos));
        spawnSpecies(world, gameMap, forest, 18, 3, rand, (pos, index) -> new Rabbit(pos));

        spawnSpecies(world, gameMap, plain, 32, 3, rand, (pos, index) -> new Deer(pos, 1 + index / 16));
        spawnSpecies(world, gameMap, forest, 22, 4, rand, (pos, index) -> new Deer(pos, 3 + index / 12));

        spawnSpecies(world, gameMap, plain, 5, 1, rand, (pos, index) -> new Elephant(pos, 1));
        spawnSpecies(world, gameMap, forest, 12, 2, rand, (pos, index) -> new Elephant(pos, 2 + index / 6));

        spawnSpecies(world, gameMap, plain, 2, 0, rand, (pos, index) -> new Tiger(pos));
        spawnSpecies(world, gameMap, forest, 12, 2, rand, (pos, index) -> new Tiger(pos));

        spawnSpecies(world, gameMap, plain, 10, 1, rand, (pos, index) -> new Wolf(pos));
        spawnSpecies(world, gameMap, forest, 18, 3, rand, (pos, index) -> new Wolf(pos));

        spawnMapWideWildlife(world, gameMap, rand, MAX_INITIAL_ANIMAL_COUNT);
    }

    private static void spawnSpecies(World world, GameMap gameMap, List<MapPolygonObject> zones,
                                     int totalCount, int minPerZone, Random rand, AnimalFactory factory) {
        if (zones == null || zones.isEmpty() || totalCount <= 0) return;

        int spawned = 0;
        int speciesIndex = 0;
        int guaranteedPerZone = Math.min(minPerZone, Math.max(0, totalCount / zones.size()));

        for (MapPolygonObject zone : zones) {
            for (int i = 0; i < guaranteedPerZone && spawned < totalCount; i++) {
                Vector2 pos = getRandomPointInPolygon(zone, gameMap, rand);
                if (pos != null) {
                    addSpawnedAnimal(world, factory.create(pos, speciesIndex++), rand);
                    spawned++;
                }
            }
        }

        while (spawned < totalCount) {
            Vector2 pos = getRandomPointInPolygons(zones, gameMap, rand);
            if (pos == null) break;
            addSpawnedAnimal(world, factory.create(pos, speciesIndex++), rand);
            spawned++;
        }
    }

    private static void addSpawnedAnimal(World world, Animal animal, Random rand) {
        double ageRatio = 0.25 + rand.nextDouble() * 0.4; // 25% - 65% vòng đời
        animal.setAge(animal.getMaxAge() * ageRatio);
        animal.setAdult(true);
        world.addEntity(animal);
    }

    private static void spawnMapWideWildlife(World world, GameMap gameMap, Random rand, int targetAnimalCount) {
        int attempts = 0;
        int maxAttempts = targetAnimalCount * 30;
        while (countAnimals(world) < targetAnimalCount && attempts < maxAttempts) {
            attempts++;
            Vector2 pos = getRandomGroundPoint(world, gameMap, rand);
            if (pos == null) break;
            addSpawnedAnimal(world, createSupplementalAnimal(pos, rand), rand);
        }
    }

    private static Animal createSupplementalAnimal(Vector2 pos, Random rand) {
        float roll = rand.nextFloat();
        if (roll < 0.36f) return new Rabbit(pos);
        if (roll < 0.68f) return new Deer(pos, 10 + rand.nextInt(4));
        if (roll < 0.84f) return new Wolf(pos);
        if (roll < 0.94f) return new Elephant(pos, 20 + rand.nextInt(3));
        return new Tiger(pos);
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
        // Grass (Rất nhiều ở Grassland)
        for (int i = 0; i < INITIAL_GRASS_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < GRASS_PLAIN_SPAWN_CHANCE;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Grass(pos));
        }

        // Mushroom (Rất nhiều ở Forest)
        for (int i = 0; i < 60; i++) {
            boolean inPlain = rand.nextFloat() < 0.2f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Mushroom(pos));
        }

        // Cây (Rất nhiều ở Forest - tạo thành rừng rậm)
        int[] normalTrees = {1, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        
        // Đồng cỏ: Cây mọc thưa
        for (int i = 0; i < 30; i++) {
            Vector2 pos = getRandomPointInPolygons(plain, gameMap, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }

        // Rừng: Mọc thành cụm rậm rạp
        for (int i = 0; i < 400; i++) {
            Vector2 pos = getRandomPointInPolygons(forest, gameMap, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }
    }

    private static void spawnStructures(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, Random rand) {
        // Bush (Nhiều ở Forest để động vật nhỏ trốn)
        for (int i = 0; i < 80; i++) {
            boolean inPlain = rand.nextFloat() < 0.3f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Bush(pos));
        }

        // Rock (Hỗn hợp)
        for (int i = 0; i < 30; i++) {
            boolean inPlain = rand.nextFloat() < 0.5f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Rock(pos));
        }
    }

    private static Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, GameMap gameMap, Random rand) {
        if (polys == null || polys.isEmpty()) return null;
        for (int attempt = 0; attempt < polys.size() * 2; attempt++) {
            Vector2 pos = getRandomPointInPolygon(selectPolygonByArea(polys, rand), gameMap, rand);
            if (pos != null) return pos;
        }
        return null;
    }

    private static Vector2 getRandomPointInPolygon(MapPolygonObject selectedPoly, GameMap gameMap, Random rand) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return null;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_POINT; attempt++) {
            float x = (float) (bounds.getX() + rand.nextDouble() * bounds.getWidth());
            float y = (float) (bounds.getY() + rand.nextDouble() * bounds.getHeight());
            if (selectedPoly.polygonPath.contains(x, y)) {
                if (gameMap != null) {
                    float m = 32.0f; // Khoảng cách an toàn tới mép nước (32px = 1 tile)
                    if (gameMap.isValidGroundSpawnPosition(x, y, m)) {
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
        if (world == null) return null;
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_POINT; attempt++) {
            float x = rand.nextFloat() * world.getWidth();
            float y = rand.nextFloat() * world.getHeight();
            if (gameMap == null || gameMap.isValidGroundSpawnPosition(x, y, 32.0f)) {
                return new Vector2(x, y);
            }
        }
        return null;
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
}

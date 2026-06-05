package model.world;

import core.Vector2;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;
import model.plants.FruitTree;
import model.plants.Grass;
import model.plants.Mushroom;
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
        // Thỏ (Nhiều ở Grassland)
        for (int i = 0; i < 60; i++) {
            boolean inPlain = rand.nextFloat() < 0.8f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Rabbit(pos));
        }

        // Hươu (Grassland)
        for (int i = 0; i < 40; i++) {
            boolean inPlain = rand.nextFloat() < 0.7f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            int herdId = (i < 20) ? 1 : 2; 
            if (pos != null) world.addEntity(new Deer(pos, herdId));
        }

        // Voi (Forest và Grassland)
        for (int i = 0; i < 15; i++) {
            boolean inPlain = rand.nextFloat() < 0.5f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Elephant(pos, 1));
        }

        // Hổ (Nhiều ở Forest)
        for (int i = 0; i < 12; i++) {
            boolean inPlain = rand.nextFloat() < 0.2f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Tiger(pos));
        }

        // Sói (Phân bố đều)
        for (int i = 0; i < 20; i++) {
            boolean inPlain = rand.nextFloat() < 0.5f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Wolf(pos));
        }
    }

    private static void spawnVegetation(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, List<MapPolygonObject> village, Random rand) {
        // Grass (Rất nhiều ở Grassland)
        for (int i = 0; i < 150; i++) {
            boolean inPlain = rand.nextFloat() < 0.9f;
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
        MapPolygonObject selectedPoly = polys.get(rand.nextInt(polys.size()));
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();

        for (int attempt = 0; attempt < 50; attempt++) {
            float x = (float) (bounds.getX() + rand.nextDouble() * bounds.getWidth());
            float y = (float) (bounds.getY() + rand.nextDouble() * bounds.getHeight());
            if (selectedPoly.polygonPath.contains(x, y)) {
                if (gameMap != null) {
                    float m = 32.0f; // Khoảng cách an toàn tới mép nước (32px = 1 tile)
                    boolean isSafe = !gameMap.isPositionInWater(x, y) &&
                                     !gameMap.isPositionInWater(x - m, y) &&
                                     !gameMap.isPositionInWater(x + m, y) &&
                                     !gameMap.isPositionInWater(x, y - m) &&
                                     !gameMap.isPositionInWater(x, y + m) &&
                                     !gameMap.isPositionInWater(x - m, y - m) &&
                                     !gameMap.isPositionInWater(x + m, y - m) &&
                                     !gameMap.isPositionInWater(x - m, y + m) &&
                                     !gameMap.isPositionInWater(x + m, y + m);
                    if (isSafe) {
                        return new Vector2(x, y);
                    }
                } else {
                    return new Vector2(x, y);
                }
            }
        }
        return null;
    }
}

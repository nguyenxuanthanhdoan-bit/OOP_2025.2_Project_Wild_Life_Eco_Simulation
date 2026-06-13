package model.world;

import core.GameConfig;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        if (gameMap.getVillagePolygons() != null) {
            villagePolygons.addAll(gameMap.getVillagePolygons());
        }
        
        while (GameConfig.getInstance().ENABLE_FALLBACK_VILLAGES
                && villagePolygons.size() < 2) {
            villagePolygons.add(createFallbackVillage(world, gameMap, rand, false));
        }
        
        if (plainPolygons.isEmpty()) plainPolygons = createFallbackZone(world, ZoneType.GRASSLAND_ZONE);
        if (forestPolygons.isEmpty()) forestPolygons = createFallbackZone(world, ZoneType.FOREST_ZONE);

        // NẾU TẮT LÀNG MẠC: Xóa sạch các vùng làng mạc để game không spawn công trình và người
        if (!GameConfig.getInstance().ENABLE_VILLAGES) {
            villagePolygons.clear();
        }

        AnimalSpawner.spawnAnimals(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        
        VegetationSpawner.spawnVegetation(world, gameMap, plainPolygons, forestPolygons, villagePolygons, rand);
        VegetationSpawner.spawnStructures(world, gameMap, plainPolygons, forestPolygons, rand);

        java.util.Map<MapPolygonObject, Settlement> settlementMap =
                VillageSpawner.spawnVillageStructures(world, gameMap, villagePolygons, rand);

        CoastalSpawner.spawnLanterns(world, gameMap, rand);
        CoastalSpawner.spawnCoastal(world, gameMap, rand);

        VillageSpawner.spawnVillagePeople(world, gameMap, settlementMap, rand);
    }

    private static MapPolygonObject createFallbackVillage(World world, GameMap gameMap, Random rand, boolean preferSand) {
        MapPolygonObject poly = new MapPolygonObject();
        poly.type = "VILLAGE";
        Path2D.Float p = new Path2D.Float();
        
        float cx = rand.nextFloat() * world.getWidth();
        float cy = rand.nextFloat() * world.getHeight();
        
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
        MapPolygonObject poly = new MapPolygonObject();
        poly.type = type.name();
        
        Path2D.Float p = new Path2D.Float();
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
}

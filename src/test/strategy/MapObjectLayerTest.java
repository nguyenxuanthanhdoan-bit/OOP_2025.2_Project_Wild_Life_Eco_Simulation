package test.strategy;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.map.GameMap;
import model.structures.DecorativeStructure;
import model.structures.House;
import model.structures.Well;
import model.world.BiomeGenerator;
import model.world.World;

import java.awt.geom.Rectangle2D;

public class MapObjectLayerTest {
    public static void main(String[] args) {
        GameMap map = new GameMap("resources/map/map2.tmx");
        GameConfig config = GameConfig.getInstance();
        assertTrue(map.getBridgePolygons().size() == 5, "Expected 5 bridge polygons");
        assertTrue(map.getVillagePolygons().size() == 2, "Expected 2 village polygons");

        Vector2 bridgePoint = findPointInside(map.getBridgePolygons().get(0));
        assertTrue(bridgePoint != null, "Expected a point inside first bridge polygon");
        assertTrue(map.isBridgeTile(bridgePoint.x, bridgePoint.y), "Bridge polygon point must be walkable bridge area");

        World world = new World();
        world.setGameMap(map);
        world.setWidth(map.getCols() * 32.0f);
        world.setHeight(map.getRows() * 32.0f);
        BiomeGenerator.generateBiomes(world, map);

        int houses = 0;
        int wells = 0;
        int decorative = 0;
        boolean usedExtendedHouseVariant = false;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof House) {
                houses++;
                String variant = entity.getImageVariant();
                if ("house_4".equals(variant) || "house_5".equals(variant) ||
                        "house_6".equals(variant) || "house_7".equals(variant) ||
                        "house_8".equals(variant)) {
                    usedExtendedHouseVariant = true;
                }
            }
            if (entity instanceof Well) wells++;
            if (entity instanceof DecorativeStructure) decorative++;
            if (entity instanceof Animal) {
                assertTrue(!isInsideAny(map.getVillagePolygons(), entity.getPosition()),
                        "Animals must not spawn inside Village polygons");
            }
        }

        assertTrue(houses >= map.getVillagePolygons().size() * config.MIN_HOUSES_PER_VILLAGE,
                "Expected 5-6 houses per village zone");
        assertTrue(usedExtendedHouseVariant, "Expected village to use house_4 or newer asset variants");
        assertTrue(wells >= 2, "Expected wells in village zones");
        assertTrue(decorative >= map.getVillagePolygons().size() * config.DECORATIONS_PER_VILLAGE,
                "Expected decorative structures in village zones");

        System.out.println("MapObjectLayerTest passed.");
    }

    private static Vector2 findPointInside(GameMap.MapPolygonObject object) {
        Rectangle2D bounds = object.polygonPath.getBounds2D();
        for (double x = bounds.getMinX(); x <= bounds.getMaxX(); x += 1.0) {
            for (double y = bounds.getMinY(); y <= bounds.getMaxY(); y += 1.0) {
                if (object.polygonPath.contains(x, y)) {
                    return new Vector2((float) x, (float) y);
                }
            }
        }
        return null;
    }

    private static boolean isInsideAny(java.util.List<GameMap.MapPolygonObject> polygons, Vector2 point) {
        if (polygons == null || point == null) return false;
        for (GameMap.MapPolygonObject polygon : polygons) {
            if (polygon != null && polygon.polygonPath != null &&
                    polygon.polygonPath.contains(point.x, point.y)) {
                return true;
            }
        }
        return false;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

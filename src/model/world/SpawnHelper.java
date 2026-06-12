package model.world;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.entity.Structure;
import model.living_beings.Animal;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Random;

public class SpawnHelper {

    public static Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, GameMap gameMap, Random rand) {
        return getRandomPointInPolygons(polys, gameMap, rand, null);
    }

    public static Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, GameMap gameMap, Random rand,
                                                    List<MapPolygonObject> excludedPolys) {
        if (polys == null || polys.isEmpty()) return null;
        for (int attempt = 0; attempt < polys.size() * 2; attempt++) {
            Vector2 pos = getRandomPointInPolygon(selectPolygonByArea(polys, rand), gameMap, rand, excludedPolys);
            if (pos != null) return pos;
        }
        return null;
    }

    public static Vector2 getRandomPointInPolygon(MapPolygonObject selectedPoly, GameMap gameMap, Random rand) {
        return getRandomPointInPolygon(selectedPoly, gameMap, rand, null);
    }

    public static Vector2 getRandomPointInPolygon(MapPolygonObject selectedPoly, GameMap gameMap, Random rand,
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

    public static Vector2 getRandomGroundPoint(World world, GameMap gameMap, Random rand) {
        return getRandomGroundPoint(world, gameMap, null, rand);
    }

    public static Vector2 getRandomGroundPoint(World world, GameMap gameMap,
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

    public static Vector2 findVillageClusterCenter(MapPolygonObject selectedPoly, Random rand) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        Vector2 center = new Vector2((float) bounds.getCenterX(), (float) bounds.getCenterY());
        if (selectedPoly.polygonPath.contains(center.x, center.y)) {
            return center;
        }
        return getRandomPointInsidePolygon(selectedPoly, rand);
    }

    public static float getVillageHomeRadius(MapPolygonObject selectedPoly) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) {
            return GameConfig.getInstance().VILLAGE_STRUCTURE_CLUSTER_RADIUS;
        }
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        float radius = (float) (Math.max(bounds.getWidth(), bounds.getHeight()) / 2.0);
        return radius + GameConfig.getInstance().VILLAGE_HOME_RADIUS_PADDING;
    }

    public static Vector2 getRandomPointInsidePolygon(MapPolygonObject selectedPoly, Random rand) {
        if (selectedPoly == null || selectedPoly.polygonPath == null) return null;
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();
        for (int attempt = 0; attempt < 1000; attempt++) {
            float x = (float) (bounds.getX() + rand.nextDouble() * bounds.getWidth());
            float y = (float) (bounds.getY() + rand.nextDouble() * bounds.getHeight());
            if (selectedPoly.polygonPath.contains(x, y)) {
                return new Vector2(x, y);
            }
        }
        return null;
    }

    public static Vector2 getRandomVillagePoint(World world, GameMap gameMap, MapPolygonObject selectedPoly,
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
        return null;
    }

    public static Vector2 randomClusteredPoint(MapPolygonObject selectedPoly, Vector2 clusterCenter, Random rand, float radiusScale) {
        if (clusterCenter == null) return getRandomPointInsidePolygon(selectedPoly, rand);
        GameConfig config = GameConfig.getInstance();
        double angle = rand.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(rand.nextDouble()) * config.VILLAGE_STRUCTURE_CLUSTER_RADIUS * radiusScale;
        return new Vector2(
                (float) (clusterCenter.x + Math.cos(angle) * radius),
                (float) (clusterCenter.y + Math.sin(angle) * radius)
        );
    }

    public static boolean isFarFromExistingStructures(World world, Vector2 pos, float minDistance) {
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

    public static boolean isInsideAnyPolygon(List<MapPolygonObject> polygons, float x, float y) {
        if (polygons == null || polygons.isEmpty()) return false;
        for (MapPolygonObject polygon : polygons) {
            if (polygon != null && polygon.polygonPath != null && polygon.polygonPath.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    public static MapPolygonObject selectPolygonByArea(List<MapPolygonObject> polys, Random rand) {
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

    public static boolean hasVillageResidentClearance(World world, Animal animal, Vector2 pos, float clearanceFactor) {
        if (world.getSpatialGrid() == null) return true;
        float clearanceRadius = animal.getSize() * clearanceFactor;
        List<Entity> nearby = world.getSpatialGrid().getNeighbors(pos, clearanceRadius);
        for (Entity entity : nearby) {
            if (entity instanceof Structure && entity.isAlive()) {
                float dist = entity.getPosition().distanceTo(pos);
                if (dist < (animal.getSize() / 2 + entity.getSize() / 2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isNearWater(GameMap gameMap, float x, float y, float maxDist) {
        int steps = (int) (maxDist / 32) + 1;
        float step = maxDist / steps;
        for (int i = 1; i <= steps; i++) {
            float r = step * i;
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

    public interface AnimalFactory {
        Animal create(Vector2 position, int index);
    }

    public interface StructureFactory {
        model.entity.Entity create(Vector2 position, int index);
    }
}

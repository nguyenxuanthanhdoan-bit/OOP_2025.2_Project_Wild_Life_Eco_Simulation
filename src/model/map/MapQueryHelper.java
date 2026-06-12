package model.map;

import java.util.List;

public class MapQueryHelper {

    public static boolean isBridgeTile(GameMap map, float worldX, float worldY) {
        if (!map.getBridgePolygons().isEmpty()) {
            return isInsidePolygonList(map.getBridgePolygons(), worldX, worldY);
        }

        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= map.getCols() || row < 0 || row >= map.getRows()) return false;
        
        if (hasTileOnLayer(map, col, row, "bridge")) return true;

        if (map.getLayersCount() > 0) {
            int rawId0 = map.getTileId(0, col, row);
            int tileId0 = rawId0 & 0x0FFFFFFF;
            boolean isWater0 = (tileId0 == 1 || (tileId0 >= 20 && tileId0 <= 37));
            if (isWater0) {
                for (int l = 1; l < map.getLayersCount(); l++) {
                    String lName = map.getLayerName(l).toLowerCase();
                    if (lName.contains("structure")) {
                        if ((map.getTileId(l, col, row) & 0x0FFFFFFF) != 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isGroundTile(GameMap map, float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= map.getCols() || row < 0 || row >= map.getRows()) return false;
        return hasTileOnLayer(map, col, row, "ground");
    }

    public static boolean isValidGroundSpawnPosition(GameMap map, float worldX, float worldY, float margin) {
        return isGroundTile(map, worldX, worldY) &&
               isGroundTile(map, worldX - margin, worldY) &&
               isGroundTile(map, worldX + margin, worldY) &&
               isGroundTile(map, worldX, worldY - margin) &&
               isGroundTile(map, worldX, worldY + margin) &&
               isGroundTile(map, worldX - margin, worldY - margin) &&
               isGroundTile(map, worldX + margin, worldY - margin) &&
               isGroundTile(map, worldX - margin, worldY + margin) &&
               isGroundTile(map, worldX + margin, worldY + margin) &&
               !isPositionInWater(map, worldX, worldY) &&
               !isPositionInWater(map, worldX - margin, worldY) &&
               !isPositionInWater(map, worldX + margin, worldY) &&
               !isPositionInWater(map, worldX, worldY - margin) &&
               !isPositionInWater(map, worldX, worldY + margin) &&
               !isPositionInWater(map, worldX - margin, worldY - margin) &&
               !isPositionInWater(map, worldX + margin, worldY - margin) &&
               !isPositionInWater(map, worldX - margin, worldY + margin) &&
               !isPositionInWater(map, worldX + margin, worldY + margin);
    }

    public static boolean hasTileOnLayer(GameMap map, int col, int row, String layerNamePart) {
        for (int l = 0; l < map.getLayersCount(); l++) {
            String lName = map.getLayerName(l).toLowerCase();
            if (lName.contains(layerNamePart)) {
                if ((map.getTileId(l, col, row) & 0x0FFFFFFF) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSandTile(GameMap map, float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= map.getCols() || row < 0 || row >= map.getRows()) return false;
        return hasTileOnLayer(map, col, row, "sand");
    }

    public static boolean isWaterTile(GameMap map, float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= map.getCols() || row < 0 || row >= map.getRows()) return true;

        int waterLayerIndex = map.getWaterLayerIndex();
        if (waterLayerIndex >= 0 && waterLayerIndex < map.getLayersCount()) {
            boolean hasWaterTile = (map.getTileId(waterLayerIndex, col, row) & 0x0FFFFFFF) != 0;
            if (!hasWaterTile) return false;

            for (int l = 0; l < map.getLayersCount(); l++) {
                if (l == waterLayerIndex) continue;
                String lName = map.getLayerName(l).toLowerCase();
                if (lName.contains("ground") || lName.contains("sand") || lName.contains("bridge")) {
                    if ((map.getTileId(l, col, row) & 0x0FFFFFFF) != 0) {
                        if (lName.contains("bridge") && !map.getBridgePolygons().isEmpty()) {
                            return !isBridgeTile(map, worldX, worldY);
                        }
                        return false;
                    }
                }
            }
            return true;
        }

        int rawId0 = map.getTileId(0, col, row);
        int tileId0 = rawId0 & 0x0FFFFFFF;
        boolean isWater = (tileId0 == 1 || (tileId0 >= 20 && tileId0 <= 37));
        if (isWater) {
            for (int l = 1; l < map.getLayersCount(); l++) {
                String lName = map.getLayerName(l).toLowerCase();
                if (lName.contains("decorate")) {
                    continue;
                }
                
                int rawId = map.getTileId(l, col, row);
                if (rawId != 0) {
                    int tileId = rawId & 0x0FFFFFFF;
                    if ((tileId >= 2 && tileId <= 19) || (tileId >= 38 && tileId < 294)) {
                        return false;
                    }
                }
            }
        }
        return isWater;
    }

    public static boolean isInsidePolygonList(List<GameMap.MapPolygonObject> polygons, float worldX, float worldY) {
        for (GameMap.MapPolygonObject poly : polygons) {
            if (poly != null && poly.polygonPath != null && poly.polygonPath.contains(worldX, worldY)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPositionInWater(GameMap map, float worldX, float worldY) {
        for (GameMap.MapPolygonObject poly : map.getBiomePolygons()) {
            if ("OCEAN".equalsIgnoreCase(poly.type) || "LAKE".equalsIgnoreCase(poly.type)) {
                if (poly.polygonPath.contains(worldX, worldY)) {
                    return true;
                }
            }
        }
        return isWaterTile(map, worldX, worldY);
    }
}

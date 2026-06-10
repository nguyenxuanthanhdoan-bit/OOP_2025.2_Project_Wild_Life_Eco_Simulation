package map;

import core.GameConfig;
import core.Vector2;
import model.map.GameMap;
import view.systems.Camera;

import java.awt.*;
import java.util.List;

public class TileMapRenderer {

    private final int TILE_SIZE;

    public TileMapRenderer() {
        this.TILE_SIZE = GameConfig.getInstance().TILE_SIZE;
    }

    public void render(Graphics2D g2d, GameMap gameMap, Camera camera) {
        if (gameMap == null) return;

        Rectangle clip = g2d.getClipBounds();
        float screenW = (clip != null) ? clip.width : 800;
        float screenH = (clip != null) ? clip.height : 600;

        camera.setViewportSize(screenW, screenH);

        float zoom = camera.getZoomLevel();
        Vector2 camPos = camera.getPosition();

        int drawSize = (int) Math.ceil(TILE_SIZE * zoom) + 1;

        int startCol = (int) Math.floor(camPos.x / TILE_SIZE);
        int endCol = (int) Math.ceil((camPos.x + screenW / zoom) / TILE_SIZE);
        int startRow = (int) Math.floor(camPos.y / TILE_SIZE);
        int endRow = (int) Math.ceil((camPos.y + screenH / zoom) / TILE_SIZE);

        startCol = Math.max(0, startCol);
        endCol = Math.min(gameMap.getCols() - 1, endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min(gameMap.getRows() - 1, endRow);

        List<GameMap.Tileset> tilesets = gameMap.getTilesets();
        
        // CHỈ RENDER CÁC TILE LAYER PHỤC VỤ HIỂN THỊ
        // Không render Object Layer của file TMX
        int layersCount = gameMap.getLayersCount();

        for (int l = 0; l < layersCount; l++) {
            for (int x = startCol; x <= endCol; x++) {
                for (int y = startRow; y <= endRow; y++) {
                    int rawTileId = gameMap.getTileId(l, x, y);
                    if (rawTileId == 0) continue; // Ô trống

                    int tileId = rawTileId & 0x0FFFFFFF;
                    if (tileId == 0) continue;

                    boolean flippedHorizontally = (rawTileId & 0x80000000) != 0;
                    boolean flippedVertically = (rawTileId & 0x40000000) != 0;
                    boolean flippedDiagonally = (rawTileId & 0x20000000) != 0;

                    Vector2 screenPos = camera.worldToScreen(new Vector2(x * TILE_SIZE, y * TILE_SIZE));

                    GameMap.Tileset currentTileset = null;
                    for (GameMap.Tileset ts : tilesets) {
                        if (tileId >= ts.firstgid) {
                            currentTileset = ts;
                            break;
                        }
                    }

                    if (currentTileset != null && currentTileset.image != null) {
                        int localId = tileId - currentTileset.firstgid;
                        int col = localId % currentTileset.columns;
                        int row = localId / currentTileset.columns;

                        int srcX = col * currentTileset.tileWidth;
                        int srcY = row * currentTileset.tileHeight;

                        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
                        g2d.translate(screenPos.x + drawSize / 2.0, screenPos.y + drawSize / 2.0);

                        if (flippedDiagonally) {
                            g2d.transform(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0));
                        }
                        if (flippedHorizontally) {
                            g2d.scale(-1, 1);
                        }
                        if (flippedVertically) {
                            g2d.scale(1, -1);
                        }

                        g2d.drawImage(currentTileset.image,
                                -drawSize / 2, -drawSize / 2,
                                drawSize / 2, drawSize / 2,
                                srcX, srcY,
                                srcX + currentTileset.tileWidth, srcY + currentTileset.tileHeight,
                                null);

                        g2d.setTransform(oldTransform);
                    }
                }
            }
        }
    }
}

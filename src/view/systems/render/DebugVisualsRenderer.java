package view.systems.render;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.world.World;
import model.map.GameMap;
import view.systems.Camera;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DebugVisualsRenderer {

    private final GameConfig config = GameConfig.getInstance();
    private final int TILE_SIZE = config.TILE_SIZE;
    private final int MINIMAP_MARGIN = config.MINIMAP_MARGIN;
    private final int MINIMAP_SIZE = config.MINIMAP_SIZE;
    private final int MINIMAP_MIN_HEIGHT = config.MINIMAP_MIN_HEIGHT;

    private BufferedImage miniMapCache;
    private BufferedImage miniMapSnowCache;
    private int miniMapSnowBucket = -1;

    private EnvironmentOverlayRenderer overlayRenderer;

    public DebugVisualsRenderer(EnvironmentOverlayRenderer overlayRenderer) {
        this.overlayRenderer = overlayRenderer;
    }

    public void renderMiniMap(World world, Graphics2D g2d, GameMap gameMap, Camera camera, RenderSystem renderSystem) {
        if (!renderSystem.isShowMiniMap() || world == null || gameMap == null) return;

        Rectangle clip = g2d.getClipBounds();
        float screenW = (clip != null) ? clip.width : 800;
        float screenH = (clip != null) ? clip.height : 600;
        float worldW = gameMap.getCols() * TILE_SIZE;
        float worldH = gameMap.getRows() * TILE_SIZE;
        if (worldW <= 0 || worldH <= 0) return;
        if (miniMapCache == null) rebuildMiniMapCache(gameMap);
        if (miniMapCache == null) return;

        int mapW = miniMapCache.getWidth();
        int mapH = miniMapCache.getHeight();
        int x0 = MINIMAP_MARGIN;
        int y0 = MINIMAP_MARGIN;

        Graphics2D g = (Graphics2D) g2d.create();
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        g.setColor(new Color(18, 24, 28, 215));
        g.fillRoundRect(x0 - 5, y0 - 5, mapW + 10, mapH + 10, 8, 8);
        g.drawImage(miniMapCache, x0, y0, null);
        updateMiniMapSnowCache(world, mapW, mapH);
        if (miniMapSnowCache != null) {
            g.drawImage(miniMapSnowCache, x0, y0, null);
        }

        // Draw entities on minimap
        if (renderSystem.isShowEntitiesOnMinimap()) {
            java.util.List<Entity> list = new java.util.ArrayList<>(world.getEntities());
            for (Entity e : list) {
                if (e == null || !e.isAlive()) continue;
                Vector2 pos = e.getPosition();
                if (pos == null) continue;
                int ex = x0 + Math.round((pos.x / worldW) * mapW);
                int ey = y0 + Math.round((pos.y / worldH) * mapH);

                if (e instanceof model.living_beings.animal.Animal) {
                    model.living_beings.animal.Animal a = (model.living_beings.animal.Animal) e;
                    if (a.getDietType() == model.living_beings.DietType.CARNIVORE) {
                        g.setColor(new Color(255, 60, 60)); // Bright Red
                    } else {
                        g.setColor(new Color(60, 230, 255)); // Bright Sky Blue
                    }
                    g.fillOval(ex - 2, ey - 2, 4, 4);
                } else if (e instanceof model.plants.Plant || e instanceof model.items.FoodSource) {
                    g.setColor(new Color(240, 240, 50)); // Bright Yellow
                    g.fillOval(ex - 1, ey - 1, 2, 2);
                }
            }
        }

        drawMiniMapCameraFrame(g, x0, y0, mapW, mapH, worldW, worldH, screenW, screenH, camera);

        g.setComposite(oldComposite);
        g.setColor(new Color(235, 245, 250, 220));
        g.drawRoundRect(x0 - 5, y0 - 5, mapW + 10, mapH + 10, 8, 8);
        g.dispose();
    }

    private void updateMiniMapSnowCache(World world, int mapW, int mapH) {
        overlayRenderer.updateSnowCoverageCache(world);
        int bucket = overlayRenderer.getSnowCoverageBucket();
        if (bucket == miniMapSnowBucket) return;
        miniMapSnowBucket = bucket;

        BufferedImage snowCoverageCache = overlayRenderer.getSnowCoverageCache();
        if (bucket == 0 || snowCoverageCache == null) {
            miniMapSnowCache = null;
            return;
        }

        if (miniMapSnowCache == null
                || miniMapSnowCache.getWidth() != mapW
                || miniMapSnowCache.getHeight() != mapH) {
            miniMapSnowCache = new BufferedImage(mapW, mapH, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g = miniMapSnowCache.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(snowCoverageCache, 0, 0, mapW, mapH, null);
        g.dispose();
    }

    public void rebuildMiniMapCache(GameMap gameMap) {
        if (gameMap == null) {
            miniMapCache = null;
            return;
        }

        float worldW = gameMap.getCols() * TILE_SIZE;
        float worldH = gameMap.getRows() * TILE_SIZE;
        if (worldW <= 0 || worldH <= 0) {
            miniMapCache = null;
            return;
        }

        int mapW = MINIMAP_SIZE;
        int mapH = Math.max(MINIMAP_MIN_HEIGHT, Math.round(MINIMAP_SIZE * (worldH / worldW)));
        BufferedImage cache = new BufferedImage(mapW, mapH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cache.createGraphics();

        int tilePixelW = Math.max(1, (int) Math.ceil(mapW / (float) gameMap.getCols()));
        int tilePixelH = Math.max(1, (int) Math.ceil(mapH / (float) gameMap.getRows()));

        for (int tx = 0; tx < gameMap.getCols(); tx++) {
            for (int ty = 0; ty < gameMap.getRows(); ty++) {
                float wx = tx * TILE_SIZE + TILE_SIZE / 2.0f;
                float wy = ty * TILE_SIZE + TILE_SIZE / 2.0f;

                if (gameMap.isBridgeTile(wx, wy)) {
                    g.setColor(new Color(140, 92, 50)); // Wooden bridge
                } else if (gameMap.isWaterTile(wx, wy)) {
                    g.setColor(new Color(18, 145, 207));
                } else if (gameMap.isGroundTile(wx, wy)) {
                    g.setColor(new Color(63, 134, 70));
                } else {
                    g.setColor(new Color(184, 148, 96));
                }

                int px = Math.round((tx / (float) gameMap.getCols()) * mapW);
                int py = Math.round((ty / (float) gameMap.getRows()) * mapH);
                g.fillRect(px, py, tilePixelW, tilePixelH);
            }
        }

        g.dispose();
        miniMapCache = cache;
        if (overlayRenderer != null) overlayRenderer.clearCache();
        miniMapSnowCache = null;
        miniMapSnowBucket = -1;
    }

    private void drawMiniMapCameraFrame(Graphics2D g, int x0, int y0, int mapW, int mapH,
                                        float worldW, float worldH, float screenW, float screenH, Camera camera) {
        Vector2 camPos = camera.getPosition();
        float zoom = camera.getZoomLevel();
        float viewW = screenW / zoom;
        float viewH = screenH / zoom;

        int frameX = x0 + Math.round((camPos.x / worldW) * mapW);
        int frameY = y0 + Math.round((camPos.y / worldH) * mapH);
        int frameW = Math.max(4, Math.round((viewW / worldW) * mapW));
        int frameH = Math.max(4, Math.round((viewH / worldH) * mapH));

        if (frameX < x0) frameX = x0;
        if (frameY < y0) frameY = y0;
        if (frameX + frameW > x0 + mapW) frameW = x0 + mapW - frameX;
        if (frameY + frameH > y0 + mapH) frameH = y0 + mapH - frameY;

        g.setStroke(new BasicStroke(2.0f));
        g.setColor(new Color(255, 255, 255, 235));
        g.drawRect(frameX, frameY, frameW, frameH);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(20, 30, 38, 230));
        g.drawRect(frameX + 1, frameY + 1, Math.max(1, frameW - 2), Math.max(1, frameH - 2));
    }

    public void clearCaches() {
        miniMapSnowCache = null;
        miniMapSnowBucket = -1;
    }
}

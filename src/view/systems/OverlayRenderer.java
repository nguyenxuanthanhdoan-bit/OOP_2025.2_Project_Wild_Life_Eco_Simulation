package view.systems;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.world.World;
import model.map.GameMap;
import model.strategies.IStrategy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;

public class OverlayRenderer {
    private final GameConfig config = GameConfig.getInstance();
    private BufferedImage snowCoverageCache;
    private int snowCoverageBucket = -1;
    private final Camera camera;

    public OverlayRenderer(Camera camera) {
        this.camera = camera;
    }

    public void setGameMap(GameMap map) {
        snowCoverageCache = null;
        snowCoverageBucket = -1;
    }

    public BufferedImage getSnowCoverageCache() {
        return snowCoverageCache;
    }

    public int getSnowCoverageBucket() {
        return snowCoverageBucket;
    }

    public float getDarknessAlpha(float timeOfDay) {
        float darknessAlpha = 0f;
        if (timeOfDay >= 18.0f && timeOfDay <= 20.0f) {
            darknessAlpha = 0.75f * ((timeOfDay - 18.0f) / 2.0f);
        } else if (timeOfDay > 20.0f || timeOfDay < 4.0f) {
            darknessAlpha = 0.75f;
        } else if (timeOfDay >= 4.0f && timeOfDay <= 6.0f) {
            darknessAlpha = 0.75f * (1.0f - ((timeOfDay - 4.0f) / 2.0f));
        }
        return darknessAlpha;
    }

    public void renderSnow(World world, GameMap gameMap, Graphics2D g2d) {
        float winterProgress = world.getWinterProgress();
        if (winterProgress <= 0.0f || gameMap == null) return;

        updateSnowCoverageCache(world);
        if (snowCoverageCache == null) return;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Rectangle clip = g2d.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, 800, 600);
        float worldWidth = gameMap.getCols() * config.TILE_SIZE;
        float worldHeight = gameMap.getRows() * config.TILE_SIZE;
        Vector2 cameraPosition = camera.getPosition();
        float zoom = camera.getZoomLevel();

        float visibleWorldX1 = Math.max(0.0f, cameraPosition.x);
        float visibleWorldY1 = Math.max(0.0f, cameraPosition.y);
        float visibleWorldX2 = Math.min(worldWidth, cameraPosition.x + clip.width / zoom);
        float visibleWorldY2 = Math.min(worldHeight, cameraPosition.y + clip.height / zoom);
        if (visibleWorldX2 <= visibleWorldX1 || visibleWorldY2 <= visibleWorldY1) {
            g.dispose();
            return;
        }

        int sourceX1 = Math.max(0, (int) Math.floor(
                visibleWorldX1 / worldWidth * snowCoverageCache.getWidth()));
        int sourceY1 = Math.max(0, (int) Math.floor(
                visibleWorldY1 / worldHeight * snowCoverageCache.getHeight()));
        int sourceX2 = Math.min(snowCoverageCache.getWidth(), (int) Math.ceil(
                visibleWorldX2 / worldWidth * snowCoverageCache.getWidth()));
        int sourceY2 = Math.min(snowCoverageCache.getHeight(), (int) Math.ceil(
                visibleWorldY2 / worldHeight * snowCoverageCache.getHeight()));

        float sourceWorldX1 = sourceX1 / (float) snowCoverageCache.getWidth() * worldWidth;
        float sourceWorldY1 = sourceY1 / (float) snowCoverageCache.getHeight() * worldHeight;
        float sourceWorldX2 = sourceX2 / (float) snowCoverageCache.getWidth() * worldWidth;
        float sourceWorldY2 = sourceY2 / (float) snowCoverageCache.getHeight() * worldHeight;
        int destinationX1 = (int) Math.floor((sourceWorldX1 - cameraPosition.x) * zoom);
        int destinationY1 = (int) Math.floor((sourceWorldY1 - cameraPosition.y) * zoom);
        int destinationX2 = (int) Math.ceil((sourceWorldX2 - cameraPosition.x) * zoom);
        int destinationY2 = (int) Math.ceil((sourceWorldY2 - cameraPosition.y) * zoom);

        g.drawImage(snowCoverageCache,
                destinationX1, destinationY1, destinationX2, destinationY2,
                sourceX1, sourceY1, sourceX2, sourceY2, null);
        g.dispose();
    }

    public void updateSnowCoverageCache(World world) {
        int bucketCount = config.SNOW_PROGRESS_BUCKETS;
        int bucket = Math.max(0,
                Math.min(bucketCount, Math.round(world.getWinterProgress() * bucketCount)));
        if (bucket == snowCoverageBucket) return;
        snowCoverageBucket = bucket;

        if (bucket == 0) {
            snowCoverageCache = null;
            return;
        }

        int width = world.getSnowCacheWidth();
        int height = world.getSnowCacheHeight();
        if (width <= 0 || height <= 0) {
            snowCoverageCache = null;
            return;
        }

        if (snowCoverageCache == null
                || snowCoverageCache.getWidth() != width
                || snowCoverageCache.getHeight() != height) {
            snowCoverageCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        int[] coveragePixels = ((DataBufferInt) snowCoverageCache
                .getRaster().getDataBuffer()).getData();
        world.fillSnowCoverage(bucket, bucketCount, coveragePixels);
    }

    public void renderNightOverlay(World world, Graphics2D g2d, List<Entity> entities) {
        float timeOfDay = world.getTimeOfDay();
        float darknessAlpha = getDarknessAlpha(timeOfDay);

        if (darknessAlpha <= 0.01f) return;

        Rectangle clip = g2d.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, 800, 600);

        BufferedImage nightBuffer = new BufferedImage(clip.width, clip.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ng = nightBuffer.createGraphics();
        ng.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ng.setColor(new Color(10, 15, 35, (int)(255 * darknessAlpha)));
        ng.fillRect(0, 0, clip.width, clip.height);

        ng.setComposite(AlphaComposite.DstOut);

        float zoom = camera.getZoomLevel();

        for (Entity e : entities) {
            if (e instanceof model.structures.Lantern) {
                Vector2 screenPos = camera.worldToScreen(e.getPosition());
                float lightRadiusBase = 180f;
                int lightRadius = (int)(lightRadiusBase * zoom * (0.2f + 0.8f * (darknessAlpha / 0.75f)));

                if (lightRadius <= 0) continue;

                float[] fractions = {0.0f, 1.0f};
                Color[] colors = {new Color(0, 0, 0, 255), new Color(0, 0, 0, 0)};

                RadialGradientPaint rgp = new RadialGradientPaint(
                    screenPos.x - clip.x, screenPos.y - clip.y, lightRadius, fractions, colors);

                ng.setPaint(rgp);
                ng.fillOval((int)(screenPos.x - clip.x - lightRadius),
                            (int)(screenPos.y - clip.y - lightRadius),
                            lightRadius * 2, lightRadius * 2);
            }
        }

        ng.dispose();
        g2d.drawImage(nightBuffer, clip.x, clip.y, null);
    }

    public void drawStatusBar(Graphics2D g2d, int x, int y, int width, int height,
                               double ratio, java.awt.Color fillColor) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillRect(x - 1, y - 1, width + 2, height + 2);
        g2d.setColor(new java.awt.Color(50, 50, 50));
        g2d.fillRect(x, y, width, height);
        g2d.setColor(fillColor);
        int fill = (int) (width * clamped);
        if (fill > 0) {
            g2d.fillRect(x, y, fill, height);
        }
    }

    public void drawCombinedStatusBar(Graphics2D g2d, int x, int y, int width, int height,
                                       double hungerRatio, double thirstRatio) {
        double clampedHunger = Math.max(0.0, Math.min(1.0, hungerRatio));
        double clampedThirst = Math.max(0.0, Math.min(1.0, thirstRatio));

        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillRect(x - 1, y - 1, width + 2, height + 2);
        g2d.setColor(new java.awt.Color(50, 50, 50));
        g2d.fillRect(x, y, width, height);

        int hungerFill = (int) (width * clampedHunger / 2);
        if (hungerFill > 0) {
            g2d.setColor(new java.awt.Color(220, 60, 60));
            g2d.fillRect(x, y, hungerFill, height);
        }

        int thirstFill = (int) (width * clampedThirst / 2);
        if (thirstFill > 0) {
            g2d.setColor(new java.awt.Color(60, 140, 240));
            g2d.fillRect(x + width - thirstFill, y, thirstFill, height);
        }

        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawLine(x + width / 2, y, x + width / 2, y + height);
    }

    public void drawHealthHearts(Graphics2D g2d, int x, int y, int barW, double currentHp, double maxHp, float zoom) {
        BufferedImage heartImg = AssetManager.getInstance().getAsset("heart");
        if (heartImg == null) return;

        int hpPerHeart = 20;
        int maxHearts = (int) Math.ceil(maxHp / hpPerHeart);
        double currentHearts = currentHp / hpPerHeart;

        int baseHeartSize = 8;
        int heartSize = Math.max(5, (int)(baseHeartSize * zoom));
        int spacing = Math.max(1, (int)(1 * zoom));
        int maxPerRow = 8;
        
        for (int i = 0; i < maxHearts; i++) {
            int row = i / maxPerRow;
            int col = i % maxPerRow;
            
            int heartsInThisRow = Math.min(maxPerRow, maxHearts - row * maxPerRow);
            int rowWidth = heartsInThisRow * heartSize + (heartsInThisRow - 1) * spacing;
            int startX = x + (barW - rowWidth) / 2;
            
            int hx = startX + col * (heartSize + spacing);
            int hy = y - row * (heartSize + spacing);

            if (currentHearts >= i + 1) {
                g2d.drawImage(heartImg, hx, hy, heartSize, heartSize, null);
            } else if (currentHearts > i) {
                double fraction = currentHearts - i;
                int partialW = (int) (heartSize * fraction);
                if (partialW > 0) {
                    g2d.drawImage(heartImg, 
                        hx, hy, hx + partialW, hy + heartSize,
                        0, 0, (int)(heartImg.getWidth() * fraction), heartImg.getHeight(),
                        null);
                }
            } else {
                Composite original = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2d.drawImage(heartImg, hx, hy, heartSize, heartSize, null);
                g2d.setComposite(original);
            }
        }
    }
}

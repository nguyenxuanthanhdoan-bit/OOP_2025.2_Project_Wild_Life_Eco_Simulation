package view.systems;

import core.Vector2;
import core.DisplayMode;
import core.TileType;
import model.entity.Entity;
import model.world.World;
import model.plants.Grass;
import model.plants.FruitTree;
import model.living_beings.Rabbit;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.map.GameMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List; // Thêm import List
import java.util.Map;

public class RenderSystem {

    private Camera camera;
    private DisplayMode displayMode;
    private Map<String, BufferedImage> assetMap;
    private float animationTimer = 0;
    private final float FRAME_DURATION = 0.15f;
    private MinimalRenderer minimalRenderer;

    private GameMap gameMap;
    private final int TILE_SIZE = 32;

    public RenderSystem(Camera camera) {
        this.camera = camera;
        this.displayMode = DisplayMode.REALISTIC;
        this.assetMap = new HashMap<>();
        this.minimalRenderer = new MinimalRenderer(camera);
        loadAssets();
    }

    public void setGameMap(GameMap map) {
        this.gameMap = map;
    }

    private void loadAssets() {
        String path = "resources/assets/images/";
        try {
            assetMap.put("rabbit", ImageIO.read(new File(path + "Rabbit_walk.png")));
            assetMap.put("tiger", ImageIO.read(new File(path + "Tiger/tiger_walk.png")));
            assetMap.put("wolf", ImageIO.read(new File(path + "Wolf/wolf_walk.png")));
            assetMap.put("grass_plant", ImageIO.read(new File(path + "Grass.png")));
            assetMap.put("tree_big", ImageIO.read(new File(path + "Oak_Tree.png")));
            assetMap.put("tree_small", ImageIO.read(new File(path + "Oak_Tree_Small.png")));
        } catch (IOException e) {
            System.err.println("Lỗi nạp ảnh: " + e.getMessage());
        }
    }

    public void renderAll(World world, Graphics2D g2d, float deltaTime) {
        animationTimer += deltaTime;

        if (displayMode == DisplayMode.MINIMAL) {
            minimalRenderer.renderBackground(g2d, world.getWidth(), world.getHeight());
        } else {
            renderMap(g2d);
        }

        // =========================================================
        // [MỚI] TỐI ƯU HÓA VẼ THỰC THỂ BẰNG SPATIAL GRID
        // =========================================================
        if (world.getSpatialGrid() != null) {
            Vector2 camPos = camera.getPosition();
            float zoom = camera.getZoomLevel();

            Rectangle clip = g2d.getClipBounds();
            float screenW = (clip != null) ? clip.width : 800;
            float screenH = (clip != null) ? clip.height : 600;

            // Tính toán tâm màn hình trong tọa độ thế giới
            Vector2 centerView = new Vector2(
                    camPos.x + (screenW / zoom) / 2f,
                    camPos.y + (screenH / zoom) / 2f
            );

            // Bán kính quét bằng nửa chiều dài đường chéo màn hình cộng thêm một chút khoảng đệm (buffer)
            float scanRange = (Math.max(screenW, screenH) / zoom) / 2f + 100f;

            // TRUY VẤN LƯỚI: Lấy danh sách thực thể nằm trong và xung quanh khung hình
            List<Entity> visibleEntities = world.getSpatialGrid().getNeighbors(centerView, scanRange);

            // Chỉ duyệt vòng lặp trên danh sách nhỏ này
            for (Entity e : visibleEntities) {
                if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                    renderEntity(e, g2d);
                }
            }
        } else {
            // [PHÒNG HỜ] Nếu Lưới chưa kịp khởi tạo thì dùng cách quét thủ công toàn bộ danh sách cũ
            for (Entity e : world.getEntities()) {
                if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                    renderEntity(e, g2d);
                }
            }
        }
    }

    private void renderMap(Graphics2D g2d) {
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

    private void renderEntity(Entity e, Graphics2D g2d) {
        if (displayMode == DisplayMode.REALISTIC) {
            Vector2 screenPos = camera.worldToScreen(e.getPosition());
            float zoom = camera.getZoomLevel();

            BufferedImage img = null;
            if (e instanceof Rabbit) drawAnimatedRabbit((Rabbit) e, g2d, screenPos, zoom);
            else if (e instanceof Tiger) drawTiger((Tiger) e, g2d, screenPos, zoom);
            else if (e instanceof Wolf) drawWolf((Wolf) e, g2d, screenPos, zoom);
            else {
                if (e instanceof Grass) img = assetMap.get("grass_plant");
                else if (e instanceof FruitTree) img = ((FruitTree) e).isSmall() ? assetMap.get("tree_small") : assetMap.get("tree_big");

                if (img != null) {
                    float aspect = (float) img.getHeight() / img.getWidth();
                    int w = (int) (e.getSize() * zoom);
                    int h = (int) (w * aspect);
                    g2d.drawImage(img, (int)screenPos.x - w/2, (int)screenPos.y - h/2, w, h, null);
                }
            }
        } else {
            minimalRenderer.renderEntity(e, g2d);
        }
    }

    private void drawAnimatedRabbit(Rabbit r, Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage sheet = assetMap.get("rabbit");
        if (sheet == null) return;

        int frameW = sheet.getWidth() / 2;
        int frameH = sheet.getHeight() / 2;
        int frameIdx = (int) (animationTimer / FRAME_DURATION) % 4;

        int drawSize = (int) (r.getSize() * zoom);

        int dstX1 = (int)screenPos.x - drawSize/2;
        int dstY1 = (int)screenPos.y - drawSize/2;
        int dstX2 = (int)screenPos.x + drawSize/2;
        int dstY2 = (int)screenPos.y + drawSize/2;

        if (r.isFacingRight()) {
            int temp = dstX1;
            dstX1 = dstX2;
            dstX2 = temp;
        }

        int srcX1 = (frameIdx % 2) * frameW;
        int srcY1 = (frameIdx / 2) * frameH;
        int srcX2 = ((frameIdx % 2) + 1) * frameW;
        int srcY2 = ((frameIdx / 2) + 1) * frameH;

        g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
    }

    private void drawTiger(Tiger t, Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage sheet = assetMap.get("tiger");
        if (sheet != null) {
            int frameW = sheet.getWidth() / 3;
            int frameH = sheet.getHeight() / 2;
            int frameIdx = (int) (animationTimer / FRAME_DURATION) % 6;

            int drawSize = (int) (t.getSize() * zoom);

            int dstX1 = (int)screenPos.x - drawSize/2;
            int dstY1 = (int)screenPos.y - drawSize/2;
            int dstX2 = (int)screenPos.x + drawSize/2;
            int dstY2 = (int)screenPos.y + drawSize/2;

            if (t.isFacingRight()) {
                int temp = dstX1;
                dstX1 = dstX2;
                dstX2 = temp;
            }

            int col = frameIdx % 3;
            int row = frameIdx / 3;

            int srcX1 = col * frameW;
            int srcY1 = row * frameH;
            int srcX2 = (col + 1) * frameW;
            int srcY2 = (row + 1) * frameH;

            g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
            return;
        }

        // Fallback Vector Rendering if image is not loaded
        int size = (int) (t.getSize() * zoom);
        int halfSize = size / 2;
        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Body
        g.setColor(new Color(235, 115, 20)); // Tiger Orange
        g.fillRoundRect(x - halfSize, y - halfSize / 2, size, (int)(size * 0.7f), 15, 15);

        // Belly
        g.setColor(new Color(250, 240, 220));
        g.fillRoundRect(x - halfSize / 2, y + (int)(size * 0.05f), halfSize * 2, (int)(size * 0.15f), 5, 5);

        // Face & direction
        boolean facingRight = t.isFacingRight();
        int headX = facingRight ? (x + halfSize / 3) : (x - halfSize / 3 - size / 4);
        int headY = y - halfSize / 3;
        int headW = (int) (size * 0.5f);
        int headH = (int) (size * 0.5f);

        // Ears
        g.setColor(new Color(40, 40, 40));
        int[] earLeftX = {headX + 5, headX + 15, headX + 8};
        int[] earLeftY = {headY, headY, headY - 12};
        int[] earRightX = {headX + headW - 15, headX + headW - 5, headX + headW - 8};
        int[] earRightY = {headY, headY, headY - 12};
        g.fillPolygon(earLeftX, earLeftY, 3);
        g.fillPolygon(earRightX, earRightY, 3);
        
        g.setColor(new Color(250, 210, 210));
        int[] innerEarLeftX = {headX + 7, headX + 13, headX + 9};
        int[] innerEarLeftY = {headY, headY, headY - 8};
        int[] innerEarRightX = {headX + headW - 13, headX + headW - 7, headX + headW - 9};
        int[] innerEarRightY = {headY, headY, headY - 8};
        g.fillPolygon(innerEarLeftX, innerEarLeftY, 3);
        g.fillPolygon(innerEarRightX, innerEarRightY, 3);

        // Head main
        g.setColor(new Color(235, 115, 20));
        g.fillOval(headX, headY, headW, headH);

        // Stripes on Head
        g.setColor(new Color(30, 30, 30));
        g.setStroke(new BasicStroke(2 * zoom));
        g.drawLine(headX + headW / 2, headY, headX + headW / 2, headY + 8);
        g.drawLine(headX + headW / 2 - 4, headY, headX + headW / 2 - 4, headY + 6);
        g.drawLine(headX + headW / 2 + 4, headY, headX + headW / 2 + 4, headY + 6);

        // Stripes on Body
        g.drawLine(x, y - halfSize / 2, x, y + (int)(size * 0.1f));
        g.drawLine(x - size / 4, y - halfSize / 2, x - size / 4, y);
        g.drawLine(x + size / 4, y - halfSize / 2, x + size / 4, y);

        // Eyes
        g.setColor(new Color(80, 200, 120));
        int eyeY = headY + headH / 3;
        int eyeSize = Math.max(3, (int) (4 * zoom));
        if (facingRight) {
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.8f), eyeY, eyeSize, eyeSize);
        } else {
            g.fillOval(headX + (int)(headW * 0.2f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
        }

        // Snout
        g.setColor(new Color(250, 240, 220));
        int snoutW = (int) (headW * 0.4f);
        int snoutH = (int) (headH * 0.3f);
        int snoutX = headX + (headW - snoutW) / 2;
        int snoutY = headY + (int)(headH * 0.5f);
        g.fillOval(snoutX, snoutY, snoutW, snoutH);
        g.setColor(Color.BLACK);
        g.fillOval(snoutX + snoutW / 2 - 2, snoutY + 1, 4, 3);

        g.dispose();
    }

    private void drawWolf(Wolf w, Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage sheet = assetMap.get("wolf");
        if (sheet != null) {
            int frameW = sheet.getWidth() / 2;
            int frameH = sheet.getHeight() / 2;
            int frameIdx = (int) (animationTimer / FRAME_DURATION) % 4;

            int drawSize = (int) (w.getSize() * zoom);

            int dstX1 = (int)screenPos.x - drawSize/2;
            int dstY1 = (int)screenPos.y - drawSize/2;
            int dstX2 = (int)screenPos.x + drawSize/2;
            int dstY2 = (int)screenPos.y + drawSize/2;

            if (w.isFacingRight()) {
                int temp = dstX1;
                dstX1 = dstX2;
                dstX2 = temp;
            }

            int col = frameIdx % 2;
            int row = frameIdx / 2;

            int srcX1 = col * frameW;
            int srcY1 = row * frameH;
            int srcX2 = (col + 1) * frameW;
            int srcY2 = (row + 1) * frameH;

            g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
            return;
        }

        // Fallback Vector Rendering if image is not loaded
        int size = (int) (w.getSize() * zoom);
        int halfSize = size / 2;
        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Bushy Tail
        boolean facingRight = w.isFacingRight();
        g.setColor(new Color(60, 70, 80));
        if (facingRight) {
            int[] tailX = {x - halfSize, x - halfSize - size / 2, x - halfSize + size / 6};
            int[] tailY = {y + size / 6, y + size / 4, y - size / 6};
            g.fillPolygon(tailX, tailY, 3);
        } else {
            int[] tailX = {x + halfSize, x + halfSize + size / 2, x + halfSize - size / 6};
            int[] tailY = {y + size / 6, y + size / 4, y - size / 6};
            g.fillPolygon(tailX, tailY, 3);
        }

        // Body
        g.setColor(new Color(75, 85, 95));
        g.fillRoundRect(x - halfSize, y - halfSize / 2, size, (int)(size * 0.7f), 12, 12);

        // Chest Mane
        g.setColor(new Color(200, 205, 210));
        int maneW = (int) (size * 0.35f);
        int maneX = facingRight ? (x + halfSize / 6) : (x - halfSize / 3 - maneW);
        g.fillRoundRect(maneX, y - halfSize / 4, maneW, (int)(size * 0.5f), 8, 8);

        // Face & direction
        int headX = facingRight ? (x + halfSize / 3) : (x - halfSize / 3 - size / 4);
        int headY = y - halfSize / 3;
        int headW = (int) (size * 0.48f);
        int headH = (int) (size * 0.48f);

        // Ears
        g.setColor(new Color(55, 65, 75));
        int[] earLeftX = {headX + 4, headX + 12, headX + 6};
        int[] earLeftY = {headY + 2, headY + 2, headY - 14};
        int[] earRightX = {headX + headW - 12, headX + headW - 4, headX + headW - 6};
        int[] earRightY = {headY + 2, headY + 2, headY - 14};
        g.fillPolygon(earLeftX, earLeftY, 3);
        g.fillPolygon(earRightX, earRightY, 3);
        
        g.setColor(new Color(110, 120, 130));
        int[] innerLeftX = {headX + 6, headX + 10, headX + 7};
        int[] innerLeftY = {headY + 2, headY + 2, headY - 9};
        int[] innerRightX = {headX + headW - 10, headX + headW - 6, headX + headW - 7};
        int[] innerRightY = {headY + 2, headY + 2, headY - 9};
        g.fillPolygon(innerLeftX, innerLeftY, 3);
        g.fillPolygon(innerRightX, innerRightY, 3);

        // Head main
        g.setColor(new Color(75, 85, 95));
        g.fillOval(headX, headY, headW, headH);

        // Glowing Amber Eyes
        g.setColor(new Color(255, 191, 0));
        int eyeY = headY + headH / 3 + 1;
        int eyeSize = Math.max(2, (int) (3 * zoom));
        if (facingRight) {
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.8f), eyeY, eyeSize, eyeSize);
        } else {
            g.fillOval(headX + (int)(headW * 0.2f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
        }

        // Snout
        g.setColor(new Color(55, 65, 75));
        int snoutW = (int) (headW * 0.5f);
        int snoutH = (int) (headH * 0.25f);
        int snoutX = facingRight ? (headX + headW - snoutW / 3) : (headX - snoutW * 2 / 3);
        int snoutY = headY + (int)(headH * 0.55f);
        g.fillRoundRect(snoutX, snoutY, snoutW, snoutH, 4, 4);

        g.dispose();
    }

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }
}
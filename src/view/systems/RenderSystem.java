package view.systems;

import core.Vector2;
import core.DisplayMode;
import core.TileType;
import model.entity.Entity;
import model.world.World;
import model.plants.Grass;
import model.plants.FruitTree;
import model.living_beings.Rabbit;
import model.living_beings.Deer;
import model.living_beings.Elephant;
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
            assetMap.put("rabbit", ImageIO.read(new File(path + "Rabbit/Rabbit_walk.png")));
            assetMap.put("deer",   ImageIO.read(new File(path + "Deer/deer_walk.png")));
            assetMap.put("elephant", ImageIO.read(new File(path + "Elephant/elephant_walk.png")));
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
            if (e instanceof Rabbit) {
                drawAnimatedSprite((model.living_beings.LivingBeing) e, g2d, screenPos, zoom, "rabbit");
            } else if (e instanceof Deer) {
                drawAnimatedSprite((model.living_beings.LivingBeing) e, g2d, screenPos, zoom, "deer");
            } else if (e instanceof Elephant) {
                drawAnimatedSprite((model.living_beings.LivingBeing) e, g2d, screenPos, zoom, "elephant");
            } else {
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

    /**
     * Vẽ hoạt ảnh chung cho mọi loài sử dụng spritesheet 2x2 (4 frame).
     * Hoán đổi dstX1/dstX2 để lật hình theo hướng mặt.
     *
     * @param being    Sinh vật cần vẽ (LivingBeing → có facingRight + size)
     * @param g2d      Đồ họa canvas
     * @param screenPos Tọa độ trên màn hình
     * @param zoom     Hệ số zoom camera
     * @param assetKey Key trong assetMap ("rabbit" / "deer" / "elephant")
     */
    private void drawAnimatedSprite(model.living_beings.LivingBeing being,
                                    Graphics2D g2d, Vector2 screenPos,
                                    float zoom, String assetKey) {
        BufferedImage sheet = assetMap.get(assetKey);
        if (sheet == null) return;

        int frameW = sheet.getWidth() / 2;
        int frameH = sheet.getHeight() / 2;
        int frameIdx = (int) (animationTimer / FRAME_DURATION) % 4;

        int drawSize = (int) (being.getSize() * zoom);

        int dstX1 = (int) screenPos.x - drawSize / 2;
        int dstY1 = (int) screenPos.y - drawSize / 2;
        int dstX2 = (int) screenPos.x + drawSize / 2;
        int dstY2 = (int) screenPos.y + drawSize / 2;

        if (being.isFacingRight()) {
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

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }
}
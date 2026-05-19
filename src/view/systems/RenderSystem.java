package view.systems;

import core.Vector2;
import core.DisplayMode;
import core.TileType;
import model.entity.Entity;
import model.entity.Structure; // [MỚI] Import class Structure cha
import model.world.World;
import model.living_beings.Rabbit;
import model.map.GameMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
            assetMap.put("grass_plant", ImageIO.read(new File(path + "Grass.png")));
            assetMap.put("tree_big", ImageIO.read(new File(path + "Oak_Tree.png")));
            // [ĐÃ XÓA] assetMap.put("tree_small", ImageIO.read(new File(path + "Oak_Tree_Small.png")));

            // --- Lời nhắn cho người làm Task Render ---
            // Nạp các ảnh PNG do PixelLab tạo ra vào đây:
            // assetMap.put("house", ImageIO.read(new File(path + "House.png")));
            // assetMap.put("fallen_fruit", ImageIO.read(new File(path + "Apple.png")));
            // assetMap.put("well", ImageIO.read(new File(path + "Well.png")));
            // ...
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

        if (world.getSpatialGrid() != null) {
            Vector2 camPos = camera.getPosition();
            float zoom = camera.getZoomLevel();

            Rectangle clip = g2d.getClipBounds();
            float screenW = (clip != null) ? clip.width : 800;
            float screenH = (clip != null) ? clip.height : 600;

            Vector2 centerView = new Vector2(
                    camPos.x + (screenW / zoom) / 2f,
                    camPos.y + (screenH / zoom) / 2f
            );

            float scanRange = (Math.max(screenW, screenH) / zoom) / 2f + 100f;
            List<Entity> visibleEntities = world.getSpatialGrid().getNeighbors(centerView, scanRange);

            for (Entity e : visibleEntities) {
                if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                    renderEntity(e, g2d);
                }
            }
        } else {
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

        for (int x = startCol; x <= endCol; x++) {
            for (int y = startRow; y <= endRow; y++) {
                TileType type = gameMap.getTile(x, y);
                Vector2 screenPos = camera.worldToScreen(new Vector2(x * TILE_SIZE, y * TILE_SIZE));

                switch (type) {
                    case OCEAN: g2d.setColor(new Color(50, 115, 215)); break;
                    case GRASS: g2d.setColor(new Color(135, 195, 65)); break;
                    case FOREST: g2d.setColor(new Color(25, 95, 15)); break;
                    case MOUNTAIN: g2d.setColor(new Color(70, 101, 93)); break;
                    case SAND: g2d.setColor(new Color(205, 200, 100)); break;
                    default: g2d.setColor(Color.BLACK); break;
                }
                g2d.fillRect((int)screenPos.x, (int)screenPos.y, drawSize, drawSize);
            }
        }
    }

    // =========================================================
    // [MỚI] TÁI CẤU TRÚC HÀM VẼ THỰC THỂ BẰNG SWITCH-CASE
    // =========================================================
    private void renderEntity(Entity e, Graphics2D g2d) {
        if (displayMode == DisplayMode.REALISTIC) {
            Vector2 screenPos = camera.worldToScreen(e.getPosition());
            float zoom = camera.getZoomLevel();

            // Nếu là động vật (Thỏ, Sói...) -> Gọi hàm vẽ Animation
            if (e instanceof Rabbit) {
                drawAnimatedRabbit((Rabbit) e, g2d, screenPos, zoom);
            }
            // Nếu là vật tĩnh (Công trình, Cây cối, Đồ rơi vãi...) -> Gọi lấy ảnh bệt
            else if (e instanceof Structure) {
                Structure struct = (Structure) e;
                BufferedImage img = null;

                // Tốc độ cao: Quét Enum thay vì ép kiểu từng Class
                switch (struct.getStructureType()) {
                    case GRASS:
                        img = assetMap.get("grass_plant");
                        break;
                    case FRUIT_TREE:
                        img = assetMap.get("tree_big"); // Bỏ cái small tree đi, dùng 1 loại thôi
                        break;
                    case HOUSE:
                        // img = assetMap.get("house");
                        break;
                    case FALLEN_FRUIT:
                        // img = assetMap.get("fallen_fruit");
                        break;
                    // ... Người làm task UI tự thêm các Case còn lại ở đây
                    default:
                        break;
                }

                // Nếu có ảnh thì vẽ ra theo tỷ lệ khung hình
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

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }
}
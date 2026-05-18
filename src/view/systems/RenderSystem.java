package view.systems;

import core.Vector2;
import core.DisplayMode;
import core.TileType; // Chú ý import Enum TileType
import model.entity.Entity;
import model.world.World;
import model.plants.Grass;
import model.plants.FruitTree;
import model.living_beings.Rabbit;
import model.map.GameMap; // Chú ý import GameMap

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RenderSystem {

    private Camera camera;
    private DisplayMode displayMode;
    private Map<String, BufferedImage> assetMap;
    private float animationTimer = 0;
    private final float FRAME_DURATION = 0.15f;
    private MinimalRenderer minimalRenderer;

    // Thêm biến chứa GameMap
    private GameMap gameMap;

    // Kích thước chuẩn của 1 ô đất trong thế giới (Ví dụ: 32)
    private final int TILE_SIZE = 32;

    public RenderSystem(Camera camera) {
        this.camera = camera;
        this.displayMode = DisplayMode.REALISTIC;
        this.assetMap = new HashMap<>();
        this.minimalRenderer = new MinimalRenderer(camera);
        loadAssets();
    }

    // Hàm Setter để truyền Map từ Controller vào
    public void setGameMap(GameMap map) {
        this.gameMap = map;
    }

    private void loadAssets() {
        String path = "resources/assets/images/";
        try {
            assetMap.put("rabbit", ImageIO.read(new File(path + "Rabbit_walk.png")));
            // assetMap.put("bg_grass", ImageIO.read(new File(path + "Grass_Middle.png"))); // Không cần nữa
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
            // SỬ DỤNG GAMEMAP ĐỂ VẼ NỀN THAY VÌ ẢNH ĐƠN
            renderMap(g2d);
        }

        for (Entity e : world.getEntities()) {
            if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                renderEntity(e, g2d);
            }
        }
    }

    // --- [MỚI] HÀM VẼ MAP TỪ DỮ LIỆU ĐỌC ĐƯỢC ---
    private void renderMap(Graphics2D g2d) {
        if (gameMap == null) return;

        // 1. Lấy kích thước thực tế của màn hình/cửa sổ game hiện tại
        Rectangle clip = g2d.getClipBounds();
        float screenW = (clip != null) ? clip.width : 800;
        float screenH = (clip != null) ? clip.height : 600;

        // 2. [QUAN TRỌNG NHẤT] Báo cho Camera biết kích thước thật ĐỂ NÓ KHÓA BIÊN
        camera.setViewportSize(screenW, screenH);

        // 3. Lấy tọa độ và độ zoom MỚI NHẤT của camera SAU KHI đã khóa biên
        float zoom = camera.getZoomLevel();
        Vector2 camPos = camera.getPosition();

        // Kích thước vẽ ra màn hình (Làm tròn lên và cộng 1 để khít mạch, không bị hở viền trắng)
        int drawSize = (int) Math.ceil(TILE_SIZE * zoom) + 1;

        // TỐI ƯU HÓA (CULLING): Vẽ tràn ra thêm 1 chút để không bị viền đen
        int startCol = (int) Math.floor(camPos.x / TILE_SIZE);
        int endCol = (int) Math.ceil((camPos.x + screenW / zoom) / TILE_SIZE);
        int startRow = (int) Math.floor(camPos.y / TILE_SIZE);
        int endRow = (int) Math.ceil((camPos.y + screenH / zoom) / TILE_SIZE);

        startCol = Math.max(0, startCol);
        endCol = Math.min(gameMap.getCols() - 1, endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min(gameMap.getRows() - 1, endRow);

        // Quét và vẽ từng ô
        for (int x = startCol; x <= endCol; x++) {
            for (int y = startRow; y <= endRow; y++) {
                TileType type = gameMap.getTile(x, y);

                // Tọa độ thực trên màn hình
                Vector2 screenPos = camera.worldToScreen(new Vector2(x * TILE_SIZE, y * TILE_SIZE));

                // Chọn màu "nhựa" tương ứng với loại đất
                switch (type) {
                    case OCEAN: g2d.setColor(new Color(50, 115, 215)); break;
                    case GRASS: g2d.setColor(new Color(135, 195, 65)); break;
                    case FOREST: g2d.setColor(new Color(25, 95, 15)); break;
                    case MOUNTAIN: g2d.setColor(Color.DARK_GRAY); break;
                    case SAND: g2d.setColor(new Color(205, 200, 100)); break;
                    default: g2d.setColor(Color.BLACK); break;
                }

                // Đổ màu bệt
                g2d.fillRect((int)screenPos.x, (int)screenPos.y, drawSize, drawSize);
            }
        }
    }

    private void renderEntity(Entity e, Graphics2D g2d) {
        if (displayMode == DisplayMode.REALISTIC) {
            Vector2 screenPos = camera.worldToScreen(e.getPosition());
            float zoom = camera.getZoomLevel();

            BufferedImage img = null;
            if (e instanceof Rabbit) drawAnimatedRabbit((Rabbit) e, g2d, screenPos, zoom);
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

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }
}
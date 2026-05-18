package view.systems;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;
import model.world.World;
import model.plants.Grass;
import model.plants.FruitTree;
import model.living_beings.Rabbit;

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

    public RenderSystem(Camera camera) {
        this.camera = camera;
        this.displayMode = DisplayMode.REALISTIC;
        this.assetMap = new HashMap<>();
        this.minimalRenderer = new MinimalRenderer(camera);
        loadAssets();
    }

    private void loadAssets() {
        String path = "resources/assets/images/";
        try {
            assetMap.put("rabbit", ImageIO.read(new File(path + "Rabbit_walk.png")));
            assetMap.put("bg_grass", ImageIO.read(new File(path + "Grass_Middle.png")));
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
            // SỬ DỤNG ẢNH GRASS_MIDDLE.PNG ĐỂ LÀM NỀN
            drawTiledBackground(world, g2d);
        }

        for (Entity e : world.getEntities()) {
            if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                renderEntity(e, g2d);
            }
        }
    }

    private void drawTiledBackground(World world, Graphics2D g2d) {
        BufferedImage tile = assetMap.get("bg_grass");
        if (tile == null) return;

        float zoom = camera.getZoomLevel();
        Vector2 camPos = camera.getPosition();

        // Kích thước ô gạch thực tế (ví dụ: 128px)
        float tileW = tile.getWidth();
        float tileH = tile.getHeight();

        // Kích thước vẽ ra màn hình (Làm tròn lên và cộng 1 để khít mạch)
        int drawW = (int) Math.ceil(tileW * zoom) + 1;
        int drawH = (int) Math.ceil(tileH * zoom) + 1;

        // Tính toán những ô gạch nào đang nằm trong khung hình để tối ưu
        int startCol = (int) Math.floor(camPos.x / tileW);
        int endCol = (int) Math.ceil((camPos.x + 800 / zoom) / tileW);
        int startRow = (int) Math.floor(camPos.y / tileH);
        int endRow = (int) Math.ceil((camPos.y + 600 / zoom) / tileH);

        // Giới hạn vòng lặp trong phạm vi World
        startCol = Math.max(0, startCol);
        endCol = Math.min((int)(world.getWidth() / tileW), endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min((int)(world.getHeight() / tileH), endRow);

        for (int x = startCol; x <= endCol; x++) {
            for (int y = startRow; y <= endRow; y++) {
                Vector2 screenPos = camera.worldToScreen(new Vector2(x * tileW, y * tileH));
                g2d.drawImage(tile, (int)screenPos.x, (int)screenPos.y, drawW, drawH, null);
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

        // 1. Tính toán Tọa độ in ra màn hình (Destination)
        int dstX1 = (int)screenPos.x - drawSize/2;
        int dstY1 = (int)screenPos.y - drawSize/2;
        int dstX2 = (int)screenPos.x + drawSize/2;
        int dstY2 = (int)screenPos.y + drawSize/2;

        // 2. LẬT ẢNH: Nếu thỏ quay sang trái, đảo ngược điểm đầu cuối của trục X
        if (r.isFacingRight()) {
            int temp = dstX1;
            dstX1 = dstX2;
            dstX2 = temp;
        }

        // 3. Tính toán Tọa độ cắt ảnh từ Sprite Sheet (Source)
        int srcX1 = (frameIdx % 2) * frameW;
        int srcY1 = (frameIdx / 2) * frameH;
        int srcX2 = ((frameIdx % 2) + 1) * frameW;
        int srcY2 = ((frameIdx / 2) + 1) * frameH;

        // 4. Vẽ ảnh (Java sẽ tự lật nếu dstX1 > dstX2)
        g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
    }

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }
}
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
        
        // Herbivores
        String[] herbivores = {"rabbit", "deer", "elephant"};
        for (String sp : herbivores) {
            String capitalizedSp = sp.substring(0, 1).toUpperCase() + sp.substring(1);
            String dirPath = path + "HerbivoreAnimal/" + capitalizedSp + "/";
            String prefix = sp.equals("rabbit") ? "Rabbit_" : (sp + "_");
            
            tryLoadAsset(sp + "_west", dirPath + "west.png");
            tryLoadAsset(sp + "_walk", dirPath + prefix + "walk.png");
            if (!sp.equals("elephant")) {
                tryLoadAsset(sp + "_run", dirPath + prefix + "run.png");
            }
            
            if (sp.equals("rabbit") || sp.equals("deer")) {
                tryLoadAsset(sp + "_eat", dirPath + sp + "_eating.png");
            } else if (sp.equals("elephant")) {
                tryLoadAsset(sp + "_eat", dirPath + sp + "_eat.png");
            }
        }
        
        // Carnivores
        String[] carnivores = {"tiger", "wolf"};
        for (String sp : carnivores) {
            String capitalizedSp = sp.substring(0, 1).toUpperCase() + sp.substring(1);
            String dirPath = path + "CarnivoreAnimal/" + capitalizedSp + "/";
            String prefix = sp + "_";
            
            tryLoadAsset(sp + "_west", dirPath + "west.png");
            tryLoadAsset(sp + "_walk", dirPath + prefix + "walk.png");
            tryLoadAsset(sp + "_run", dirPath + prefix + "run.png");
            
            if (sp.equals("tiger")) {
                tryLoadAsset(sp + "_attack", dirPath + "tiger_attack.png");
                tryLoadAsset(sp + "_drink", dirPath + "tiger_drink.png");
            }
        }

        // Plants
        for (int i = 1; i <= 2; i++) tryLoadAsset("grass_" + i, path + "Plant/Grass/Grass_" + i + ".png");
        for (int i = 1; i <= 13; i++) tryLoadAsset("tree_" + i, path + "Plant/Tree/Tree_" + i + ".png");
        for (int i = 1; i <= 8; i++) tryLoadAsset("mushroom_" + i, path + "Plant/Mushrooms/Mushroom_" + i + ".png");

        // Structures
        for (int i = 1; i <= 2; i++) tryLoadAsset("bush_" + i, path + "Structures/Bush/Bush_" + i + ".png");
        for (int i = 1; i <= 3; i++) tryLoadAsset("rock_" + i, path + "Structures/Rock/Rock_" + i + ".png");

        // Items
        for (int i = 1; i <= 2; i++) tryLoadAsset("fruit_" + i, path + "Items/Fruit/Fruit_" + i + ".png");
        tryLoadAsset("meat", path + "Items/Meat/Meat.png");
        tryLoadAsset("bone", path + "Items/Bone/Bone.png");
    }

    private void tryLoadAsset(String key, String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                assetMap.put(key, ImageIO.read(f));
            }
        } catch (IOException e) {
            System.err.println("Không thể nạp: " + path);
        }
    }

    public void renderAll(World world, Graphics2D g2d, float deltaTime) {
        animationTimer += deltaTime;

        // Luôn luôn vẽ map dù ở chế độ REALISTIC hay MINIMAL
        renderMap(g2d);

        // =========================================================
        // [MỚI] TỐI ƯU HÓA VẼ THỰC THỂ BẰNG SPATIAL GRID VÀ LAYERS
        // =========================================================
        List<Entity> entitiesToRender = new java.util.ArrayList<>();
        
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
            entitiesToRender = world.getSpatialGrid().getNeighbors(centerView, scanRange);
        } else {
            entitiesToRender = world.getEntities();
        }

        List<Entity> groundLayer = new java.util.ArrayList<>();
        List<Entity> animalLayer = new java.util.ArrayList<>();
        List<Entity> topLayer = new java.util.ArrayList<>();

        for (Entity e : entitiesToRender) {
            if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                if (e instanceof model.structures.Bush || e instanceof model.plants.FruitTree) {
                    topLayer.add(e);
                } else if (e instanceof model.living_beings.Animal) {
                    model.living_beings.Animal a = (model.living_beings.Animal) e;
                    if (a.isHidden()) {
                        // Núp trong bụi, vẽ dưới Bush
                        groundLayer.add(e);
                    } else {
                        animalLayer.add(e);
                    }
                } else {
                    groundLayer.add(e);
                }
            }
        }

        // Vẽ theo thứ tự từ dưới lên trên
        for (Entity e : groundLayer) renderEntity(e, g2d);
        for (Entity e : animalLayer) renderEntity(e, g2d);
        for (Entity e : topLayer) renderEntity(e, g2d);
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

            if (e instanceof model.living_beings.Animal) {
                model.living_beings.Animal animal = (model.living_beings.Animal) e;
                drawDynamicAnimatedSprite(animal, g2d, screenPos, zoom);

                // Vẽ thanh trạng thái (đói và khát) khi zoom to đủ
                if (zoom >= 1.2f) {
                    int barW = (int)(25 * zoom);
                    int barH = Math.max(3, (int)(4 * zoom));
                    int barX = (int)screenPos.x - barW / 2;
                    int barY = (int)screenPos.y - (int)((animal.getSize() / 2 + 10) * zoom);

                    // Thanh Đói (Đỏ)
                    g2d.setColor(java.awt.Color.BLACK);
                    g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);
                    g2d.setColor(new java.awt.Color(50, 50, 50));
                    g2d.fillRect(barX, barY, barW, barH);
                    g2d.setColor(new java.awt.Color(220, 60, 60));
                    int hungerFill = (int)(barW * (animal.getHunger() / animal.getMaxHunger()));
                    if (hungerFill > 0) g2d.fillRect(barX, barY, hungerFill, barH);

                    // Thanh Khát (Xanh) — ngay dưới thanh đói
                    int thirstY = barY + barH + 1;
                    g2d.setColor(java.awt.Color.BLACK);
                    g2d.fillRect(barX - 1, thirstY - 1, barW + 2, barH + 2);
                    g2d.setColor(new java.awt.Color(50, 50, 50));
                    g2d.fillRect(barX, thirstY, barW, barH);
                    g2d.setColor(new java.awt.Color(60, 140, 240));
                    int thirstFill = (int)(barW * (animal.getThirst() / animal.getMaxThirst()));
                    if (thirstFill > 0) g2d.fillRect(barX, thirstY, thirstFill, barH);
                }
            } else {
                BufferedImage img = null;
                String variant = e.getImageVariant();
                if (variant != null && !variant.isEmpty()) {
                    img = assetMap.get(variant.toLowerCase());
                }

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
     * Tự động tính toán khung hình (grid) dựa trên kích thước west.png làm chuẩn.
     */
    private void drawDynamicAnimatedSprite(model.living_beings.Animal animal,
                                           Graphics2D g2d, Vector2 screenPos,
                                           float zoom) {
        // Xác định loại động vật (tên class)
        String species = animal.getClass().getSimpleName().toLowerCase();
        
        // Suy diễn trạng thái
        String state = animal.getActionState();
        if (state == null || state.equals("idle")) {
            state = "west";
            if (animal.isMoving()) {
                float speed = animal.getSpeed();
                if (speed > animal.getBaseSpeed() * 1.1f) {
                    state = "run";
                } else {
                    state = "walk";
                }
            }
        }
        
        // [CÓ THỂ MỞ RỘNG] Nếu animal có thuộc tính isEating() hay isSleeping() thì gắn state tương ứng ở đây
        
        // Lấy spritesheet
        BufferedImage sheet = assetMap.get(species + "_" + state);
        if (sheet == null) {
            if (state.equals("attack")) {
                sheet = assetMap.get(species + "_west");
            }
            if (sheet == null) sheet = assetMap.get(species + "_walk");
            if (sheet == null) sheet = assetMap.get(species + "_west");
        }
        if (sheet == null) return;
        
        // Lấy frame chuẩn
        BufferedImage base = assetMap.get(species + "_west");
        if (base == null) base = sheet; // Dự phòng
        
        int frameW = base.getWidth();
        int frameH = base.getHeight();
        int cols = sheet.getWidth() / frameW;
        int rows = sheet.getHeight() / frameH;
        int totalFrames = cols * rows;
        if (totalFrames <= 0) totalFrames = 1;
        
        int frameIdx = 0;
        if (totalFrames > 1) {
            // Chỉnh tốc độ hoạt ảnh theo trạng thái
            float animSpeed = (state.equals("run")) ? FRAME_DURATION * 0.6f : FRAME_DURATION;
            frameIdx = (int) (animationTimer / animSpeed) % totalFrames;
        }

        int drawSize = (int) (animal.getSize() * zoom);

        int dstX1 = (int) screenPos.x - drawSize / 2;
        int dstY1 = (int) screenPos.y - drawSize / 2;
        int dstX2 = (int) screenPos.x + drawSize / 2;
        int dstY2 = (int) screenPos.y + drawSize / 2;

        if (animal.isFacingRight()) {
            int temp = dstX1;
            dstX1 = dstX2;
            dstX2 = temp;
        }

        int srcX1 = (frameIdx % cols) * frameW;
        int srcY1 = (frameIdx / cols) * frameH;
        int srcX2 = srcX1 + frameW;
        int srcY2 = srcY1 + frameH;

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
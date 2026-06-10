package view.systems;

import core.Vector2;
import core.DisplayMode;
import core.TileType;
import core.GameConfig;
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
import map.TileMapRenderer;

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
    private final GameConfig config = GameConfig.getInstance();
    private final float FRAME_DURATION = config.ANIMATION_FRAME_DURATION;
    private MinimalRenderer minimalRenderer;
    private TileMapRenderer tileMapRenderer;

    private GameMap gameMap;
    private final int TILE_SIZE = config.TILE_SIZE;
    private final int MINIMAP_MARGIN = config.MINIMAP_MARGIN;
    private final int MINIMAP_SIZE = config.MINIMAP_SIZE;
    private final int MINIMAP_MIN_HEIGHT = config.MINIMAP_MIN_HEIGHT;
    private final float STATUS_BAR_MIN_ZOOM = config.STATUS_BAR_MIN_ZOOM;
    private BufferedImage miniMapCache;

    private boolean showHungerBar = true;
    private boolean showThirstBar = true;
    private boolean showMiniMap = true;
    private boolean showSpeciesName = false;
    private boolean showDebugPath = false;
    private boolean showAIVision = false;
    private boolean showEntitiesOnMinimap = false;
    private model.living_beings.Animal selectedAnimal = null;

    public RenderSystem(Camera camera) {
        this.camera = camera;
        this.displayMode = DisplayMode.REALISTIC;
        this.assetMap = new HashMap<>();
        this.minimalRenderer = new MinimalRenderer(camera);
        this.tileMapRenderer = new TileMapRenderer();
        loadAssets();
    }

    public void setGameMap(GameMap map) {
        this.gameMap = map;
        rebuildMiniMapCache();
    }

    private void loadAssets() {
        String path = "resources/assets/images/";

        // Animals & Fish (Refactored to use registerSpecies)
        String[] herbivores = {"rabbit", "deer", "elephant"};
        for (String sp : herbivores) registerSpecies(sp, "HerbivoreAnimal");

        String[] carnivores = {"tiger", "wolf"};
        for (String sp : carnivores) registerSpecies(sp, "CarnivoreAnimal");

        String[] fishSpecies = {"clownfish", "shark", "sunfish"};
        for (String sp : fishSpecies) registerSpecies(sp, "Fish");

        loadHumanAssets("human_male", path + "Human/Male/");
        loadHumanAssets("human_female", path + "Human/Female/");
        loadHumanAssets("human_hunter", path + "Human/Hunter/");
        tryLoadAsset("fireball", path + "Human/Hunter/fireball.png");
        tryLoadAsset("dart", path + "Human/Hunter/dart.png");

        // Plants
        for (int i = 1; i <= 2; i++) tryLoadAsset("grass_" + i, path + "Plant/Grass/Grass_" + i + ".png");
        for (int i = 1; i <= 13; i++) tryLoadAsset("tree_" + i, path + "Plant/Tree/Tree_" + i + ".png");
        for (int i = 1; i <= 8; i++) tryLoadAsset("mushroom_" + i, path + "Plant/Mushrooms/Mushroom_" + i + ".png");

        // Structures
        for (int i = 1; i <= 2; i++) tryLoadAsset("bush_" + i, path + "Structures/Bush/Bush_" + i + ".png");
        for (int i = 1; i <= 3; i++) tryLoadAsset("rock_" + i, path + "Structures/Rock/Rock_" + i + ".png");
        
        loadAssetsFromDirectory(path + "village/");

        // Items
        for (int i = 1; i <= 2; i++) tryLoadAsset("fruit_" + i, path + "Items/Fruit/Fruit_" + i + ".png");
        tryLoadAsset("carcass", path + "Items/carcass.png");
        tryLoadAsset("bone", path + "Items/bone.png");
        tryLoadAsset("egg", path + "Items/egg.png");
        tryLoadAsset("fish", path + "Items/fish.png");
        tryLoadAsset("fishing_net", path + "Items/fishing_net.png");

        // Lanterns
        tryLoadAsset("lantern", path + "Structures/Lantern/lantern.png");
    }

    public void registerSpecies(String species, String category) {
        if (assetMap.containsKey(species + "_west")) return; // already loaded

        String path = "resources/assets/images/";
        if (category.equals("Fish")) {
            tryLoadAsset(species + "_west", path + "Fish/" + species + ".png");
            return;
        }

        String capitalizedSp = species.substring(0, 1).toUpperCase() + species.substring(1);
        String dirPath = path + category + "/" + capitalizedSp + "/";
        String prefix = species.equals("rabbit") ? "Rabbit_" : (species + "_");

        tryLoadAsset(species + "_west", dirPath + "west.png");
        tryLoadAsset(species + "_walk", dirPath + prefix + "walk.png");
        if (!species.equals("elephant")) {
            tryLoadAsset(species + "_run", dirPath + prefix + "run.png");
        }

        if (species.equals("rabbit") || species.equals("deer") || species.equals("wolf")) {
            tryLoadAsset(species + "_eat", dirPath + species + "_eating.png");
        }
        if (species.equals("elephant") || species.equals("tiger") || species.equals("wolf")) {
            tryLoadAsset(species + "_eat", dirPath + species + "_eat.png");
        }

        tryLoadAsset(species + "_drink", dirPath + species + "_drink.png");
        tryLoadAsset(species + "_drink", dirPath + species + "_drinking.png");
        tryLoadAsset(species + "_sleep", dirPath + "sleep.png");
        tryLoadAsset(species + "_sleep", dirPath + prefix + "sleep.png");

        if (species.equals("tiger")) {
            tryLoadAsset(species + "_attack", dirPath + "tiger_attack.png");
            tryLoadAsset(species + "_drink", dirPath + "tiger_drink.png");
        }
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

    private void loadAssetsFromDirectory(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles((parent, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex <= 0) continue;
            String key = name.substring(0, dotIndex).toLowerCase();
            tryLoadAsset(key, file.getPath());
        }
    }

    private void loadHumanAssets(String keyPrefix, String dirPath) {
        tryLoadAsset(keyPrefix + "_idle", dirPath + "idle.png");
        tryLoadAsset(keyPrefix + "_west", dirPath + "idle.png");
        tryLoadAsset(keyPrefix + "_walk", dirPath + "walk.png");
        tryLoadAsset(keyPrefix + "_run", dirPath + "run.png");
        tryLoadAsset(keyPrefix + "_eat", dirPath + "eat.png");
        tryLoadAsset(keyPrefix + "_drink", dirPath + "eat.png");
        tryLoadAsset(keyPrefix + "_attack", dirPath + "fireball.png");
    }

    public void renderAll(World world, Graphics2D g2d, float deltaTime) {
        animationTimer += deltaTime;

        // Luôn luôn vẽ map dù ở chế độ REALISTIC hay MINIMAL
        tileMapRenderer.render(g2d, gameMap, camera);

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
        List<Entity> worldLayer = new java.util.ArrayList<>();
        List<Entity> topLayer = new java.util.ArrayList<>();

        for (Entity e : entitiesToRender) {
            if (camera.isVisible(e.getPosition(), e.getSize() * 3)) {
                if (e instanceof model.items.FireballProjectile) {
                    topLayer.add(e);
                } else if (e instanceof model.structures.Bush ||
                        e instanceof model.plants.FruitTree ||
                        e instanceof model.entity.Structure ||
                        e instanceof model.structures.Lantern) {
                    worldLayer.add(e);
                } else if (e instanceof model.living_beings.Fish) {
                    // Cá luôn ở dưới nước nên vẽ ở groundLayer để bị lấp bởi thuyền
                    groundLayer.add(e);
                } else if (e instanceof model.living_beings.Animal) {
                    model.living_beings.Animal a = (model.living_beings.Animal) e;
                    if (a.isHidden()) {
                        // Núp trong bụi, vẽ dưới Bush
                        groundLayer.add(e);
                    } else {
                        worldLayer.add(e);
                    }
                } else {
                    groundLayer.add(e);
                }
            }
        }

        worldLayer.sort(java.util.Comparator.comparingDouble(e -> e.getPosition().y + e.getSize() * 0.5f));

        for (Entity e : groundLayer) renderEntity(e, g2d);
        for (Entity e : worldLayer) renderEntity(e, g2d);
        for (Entity e : topLayer) renderEntity(e, g2d);

        renderNightOverlay(world, g2d, entitiesToRender);

        renderMiniMap(world, g2d);
    }

    private float getDarknessAlpha(float timeOfDay) {
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

    private void renderNightOverlay(World world, Graphics2D g2d, List<Entity> entities) {
        float timeOfDay = world.getTimeOfDay();
        float darknessAlpha = getDarknessAlpha(timeOfDay);

        if (darknessAlpha <= 0.01f) return; // Không cần vẽ nếu trời quá sáng

        Rectangle clip = g2d.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, 800, 600);

        // Tạo buffer cho màn đêm
        BufferedImage nightBuffer = new BufferedImage(clip.width, clip.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ng = nightBuffer.createGraphics();
        ng.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Vẽ màu nền đêm (Xanh đen đậm)
        ng.setColor(new Color(10, 15, 35, (int)(255 * darknessAlpha)));
        ng.fillRect(0, 0, clip.width, clip.height);

        // Đục lỗ ánh sáng
        ng.setComposite(AlphaComposite.DstOut);

        float zoom = camera.getZoomLevel();

        for (Entity e : entities) {
            if (e instanceof model.structures.Lantern) {
                Vector2 screenPos = camera.worldToScreen(e.getPosition());
                float lightRadiusBase = 180f;
                // Ánh sáng tỏa dần theo độ tối
                int lightRadius = (int)(lightRadiusBase * zoom * (0.2f + 0.8f * (darknessAlpha / 0.75f)));

                // Mở rộng bán kính thêm chút để ánh sáng mềm hơn
                if (lightRadius <= 0) continue;

                float[] fractions = {0.0f, 1.0f};
                // Tâm sáng nhất (xóa nhiều darkness nhất), viền mờ dần
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

        // Vẽ buffer màn đêm lên màn hình
        g2d.drawImage(nightBuffer, clip.x, clip.y, null);
    }
    private void renderEntity(Entity e, Graphics2D g2d) {
        if (displayMode == DisplayMode.REALISTIC) {
            Vector2 screenPos = camera.worldToScreen(e.getPosition());
            float zoom = camera.getZoomLevel();

            if (e instanceof model.living_beings.Animal) {
                model.living_beings.Animal animal = (model.living_beings.Animal) e;
                if (animal.isHidden()) return;

                drawDynamicAnimatedSprite(animal, g2d, screenPos, zoom);

                // Highlight selected animal
                if (animal == selectedAnimal) {
                    g2d.setColor(new Color(255, 235, 60, 200)); // Glowing gold
                    g2d.setStroke(new BasicStroke(Math.max(2.0f, 3.0f * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6f, 6f}, (float)(animationTimer * 20.0f) % 12f));
                    int selSize = (int) ((animal.getSize() + 10) * zoom);
                    g2d.drawOval((int)screenPos.x - selSize/2, (int)screenPos.y - selSize/2, selSize, selSize);
                }

                // Draw AI Vision Range
                if (showAIVision) {
                    g2d.setColor(new Color(100, 255, 100, 20)); // Light transparent green
                    int radius = (int)(animal.getVisionRange() * zoom);
                    g2d.fillOval((int)screenPos.x - radius, (int)screenPos.y - radius, radius * 2, radius * 2);
                    g2d.setColor(new Color(100, 255, 100, 90));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawOval((int)screenPos.x - radius, (int)screenPos.y - radius, radius * 2, radius * 2);
                }

                // Draw Debug Path
                if (showDebugPath && animal.getCurrentStrategy() != null) {
                    java.util.List<Vector2> path = animal.getCurrentStrategy().getPath();
                    if (path != null && !path.isEmpty()) {
                        g2d.setColor(new Color(255, 200, 0, 160)); // Orange path line
                        g2d.setStroke(new BasicStroke(Math.max(1.5f, 2.0f * zoom)));
                        Vector2 prev = camera.worldToScreen(animal.getPosition());
                        for (Vector2 wp : path) {
                            Vector2 scrWp = camera.worldToScreen(wp);
                            g2d.drawLine((int)prev.x, (int)prev.y, (int)scrWp.x, (int)scrWp.y);
                            g2d.fillOval((int)scrWp.x - 3, (int)scrWp.y - 3, 6, 6);
                            prev = scrWp;
                        }
                        Vector2 targetPos = animal.getCurrentStrategy().getTarget();
                        if (targetPos != null) {
                            Vector2 scrTarget = camera.worldToScreen(targetPos);
                            g2d.setColor(new Color(255, 50, 50, 160)); // Red target line
                            g2d.drawLine((int)prev.x, (int)prev.y, (int)scrTarget.x, (int)scrTarget.y);
                            g2d.fillRect((int)scrTarget.x - 4, (int)scrTarget.y - 4, 8, 8);
                        }
                    }
                }

                if (zoom >= STATUS_BAR_MIN_ZOOM && (showHungerBar || showThirstBar || showSpeciesName)) {
                    int barW = Math.max(30, (int)(42 * zoom));
                    int barH = Math.max(2, Math.min(5, Math.round(2.4f * zoom)));
                    int barX = (int)screenPos.x - barW / 2;
                    int barY = (int)screenPos.y - (int)((animal.getSize() / 2 + 10) * zoom);

                    int currentY = barY;

                    if (showSpeciesName) {
                        g2d.setFont(new Font("SansSerif", Font.BOLD, (int)Math.max(10, 11 * zoom)));
                        String text = animal.getSpeciesName() + (animal.isAdult() ? "" : " (Child)");
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = (int)screenPos.x - fm.stringWidth(text) / 2;
                        int textY = currentY - 4;
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(text, textX + 1, textY + 1);
                        g2d.setColor(new Color(230, 240, 255));
                        g2d.drawString(text, textX, textY);
                    }

                    if (showHungerBar) {
                        drawStatusBar(g2d, barX, currentY, barW, barH,
                                animal.getHunger() / animal.getMaxHunger(),
                                new java.awt.Color(220, 60, 60));
                        currentY += barH + 2;
                    }

                    if (showThirstBar) {
                        drawStatusBar(g2d, barX, currentY, barW, barH,
                                animal.getThirst() / animal.getMaxThirst(),
                                new java.awt.Color(60, 140, 240));
                    }
                }
            } else if (e instanceof model.items.FireballProjectile) {
                drawHunterProjectile((model.items.FireballProjectile) e, g2d, screenPos, zoom);
            } else if (e instanceof model.structures.Lantern) {
                drawLantern((model.structures.Lantern) e, g2d, screenPos, zoom);
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
                    
                    // Nếu là Thuyền và đang thả lưới
                    if (e instanceof model.structures.Boat) {
                        model.structures.Boat boat = (model.structures.Boat) e;
                        if (boat.getState() == model.structures.Boat.BoatState.FISHING) {
                            BufferedImage net = assetMap.get("fishing_net");
                            if (net != null) {
                                // Vẽ lưới kế bên thuyền, kích thước 32x32
                                int netW = (int) (32 * zoom);
                                int netH = (int) (32 * zoom);
                                g2d.drawImage(net, (int)screenPos.x + w/2, (int)screenPos.y - netH/2, netW, netH, null);
                            }
                        }
                    }
                }
            }
        } else {
            minimalRenderer.renderEntity(e, g2d);
        }
    }

    private void drawLantern(model.structures.Lantern lantern, Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage sheet = assetMap.get("lantern");
        if (sheet == null) return;

        int frameWidth = 30;
        int frameHeight = 49;
        int columns = Math.max(1, sheet.getWidth() / frameWidth);

        float darknessAlpha = 0f;
        if (lantern.getWorld() != null) {
            darknessAlpha = getDarknessAlpha(lantern.getWorld().getTimeOfDay());
        }

        int frameIndex;
        if (darknessAlpha <= 0.01f) {
            frameIndex = 0;
        } else {
            int animatedFrames = Math.max(1, Math.min(2, columns - 1));
            frameIndex = 1 + ((int)(lantern.getAnimationTime() * 5) % animatedFrames);
        }

        int col = frameIndex % columns;
        int row = frameIndex / columns;
        int srcX = col * frameWidth;
        int srcY = row * frameHeight;

        int w = (int) (16 * zoom);
        int h = (int) (16 * 49f / 30f * zoom);

        g2d.drawImage(sheet,
                (int)screenPos.x - w / 2, (int)screenPos.y - h / 2,
                (int)screenPos.x + w / 2, (int)screenPos.y + h / 2,
                srcX, srcY, srcX + frameWidth, srcY + frameHeight, null);
    }

    private void drawStatusBar(Graphics2D g2d, int x, int y, int width, int height,
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

    private void drawHunterProjectile(model.items.FireballProjectile projectile,
                                      Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage dart = assetMap.get("dart");
        if (dart != null) {
            int drawSize = Math.max(8, Math.round(projectile.getSize() * zoom));
            java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(screenPos.x, screenPos.y);
            g2d.rotate(projectile.getRotationRadians());
            g2d.drawImage(dart, -drawSize / 2, -drawSize / 2, drawSize, drawSize, null);
            g2d.setTransform(oldTransform);
            return;
        }

        Entity e = projectile;
        int size = Math.max(8, Math.round(e.getSize() * zoom));
        int glow = Math.max(size + 8, Math.round(size * 1.8f));

        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2d.setColor(new Color(255, 80, 25));
        g2d.fillOval((int) screenPos.x - glow / 2, (int) screenPos.y - glow / 2, glow, glow);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        g2d.setColor(new Color(220, 20, 20));
        g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);

        int core = Math.max(3, size / 2);
        g2d.setColor(new Color(255, 210, 80));
        g2d.fillOval((int) screenPos.x - core / 2, (int) screenPos.y - core / 2, core, core);
        g2d.setComposite(oldComposite);
    }

    private void renderMiniMap(World world, Graphics2D g2d) {
        if (!showMiniMap || world == null || gameMap == null) return;

        Rectangle clip = g2d.getClipBounds();
        float screenW = (clip != null) ? clip.width : 800;
        float screenH = (clip != null) ? clip.height : 600;
        float worldW = gameMap.getCols() * TILE_SIZE;
        float worldH = gameMap.getRows() * TILE_SIZE;
        if (worldW <= 0 || worldH <= 0) return;
        if (miniMapCache == null) rebuildMiniMapCache();
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

        // Draw entities on minimap
        if (showEntitiesOnMinimap) {
            java.util.List<Entity> list = new java.util.ArrayList<>(world.getEntities());
            for (Entity e : list) {
                if (e == null || !e.isAlive()) continue;
                Vector2 pos = e.getPosition();
                if (pos == null) continue;
                int ex = x0 + Math.round((pos.x / worldW) * mapW);
                int ey = y0 + Math.round((pos.y / worldH) * mapH);

                if (e instanceof model.living_beings.Animal) {
                    model.living_beings.Animal a = (model.living_beings.Animal) e;
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

        drawMiniMapCameraFrame(g, x0, y0, mapW, mapH, worldW, worldH, screenW, screenH);

        g.setComposite(oldComposite);
        g.setColor(new Color(235, 245, 250, 220));
        g.drawRoundRect(x0 - 5, y0 - 5, mapW + 10, mapH + 10, 8, 8);
        g.dispose();
    }

    public void rebuildMiniMapCache() {
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
    }

    private void drawMiniMapCameraFrame(Graphics2D g, int x0, int y0, int mapW, int mapH,
                                        float worldW, float worldH, float screenW, float screenH) {
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

    /**
     * Tự động tính toán khung hình (grid) dựa trên kích thước west.png làm chuẩn.
     */
    private void drawDynamicAnimatedSprite(model.living_beings.Animal animal,
                                           Graphics2D g2d, Vector2 screenPos,
                                           float zoom) {
        // Xác định loại động vật (tên class)
        String species = animal.getSpriteKey();

        // Suy diễn trạng thái
        String state = animal.getActionState();
        boolean moving = animal.isMoving();
        if ("attack".equals(state) || "eat".equals(state) || "drink".equals(state) || "sleep".equals(state)) {
            // Giữ các animation hành động ngay cả khi đứng tại chỗ.
        } else if (moving) {
            if ("run".equals(state) || animal.getSpeed() > animal.getBaseSpeed() * 1.1f) {
                state = "run";
            } else {
                state = "walk";
            }
        } else {
            state = "idle";
        }

        // [CÓ THỂ MỞ RỘNG] Nếu animal có thuộc tính isEating() hay isSleeping() thì gắn state tương ứng ở đây

        // Lấy spritesheet
        BufferedImage sheet = ("eat".equals(state) || "drink".equals(state))
                ? getEatDrinkSheet(species)
                : assetMap.get(species + "_" + state);
        if (sheet == null) {
            if (state.equals("attack") && "human_hunter".equals(species)) {
                sheet = assetMap.get(species + "_idle");
            }
            if (sheet == null && state.equals("attack")) {
                // Fallback: dùng "run" khi attack (trông chân thực hơn "west" đứng yên)
                sheet = assetMap.get(species + "_run");
            }
            if (sheet == null && state.equals("idle")) sheet = assetMap.get(species + "_west");
            if (sheet == null) sheet = assetMap.get(species + "_walk");
            if (sheet == null) sheet = assetMap.get(species + "_west");

            // [MỚI] Fallback cho cá (Fish) hoặc các Entity chỉ có 1 hình ảnh tĩnh mang tên "species.png"
            if (sheet == null) sheet = assetMap.get(species + ".png");
        }
        if (sheet == null) return;

        // Lấy frame chuẩn để tham chiếu kích thước của các hoạt ảnh chuẩn (walk, run, v.v.)
        BufferedImage base = assetMap.get(species + "_west");
        if (base == null) base = sheet; // Dự phòng

        int baseFrameW = Math.max(1, base.getWidth());
        int baseFrameH = Math.max(1, base.getHeight());
        int cols = Math.max(1, Math.round(sheet.getWidth() / (float) baseFrameW));
        int rows = Math.max(1, Math.round(sheet.getHeight() / (float) baseFrameH));
        int frameW = Math.max(1, sheet.getWidth() / cols);
        int frameH = Math.max(1, sheet.getHeight() / rows);

        if ("sleep".equals(state)) {
            frameH = Math.max(1, sheet.getHeight());
            frameW = Math.max(1, Math.min(frameH, sheet.getWidth()));
            cols = Math.max(1, sheet.getWidth() / frameW);
            rows = 1;
        }
        int totalFrames = cols * rows;
        if (totalFrames <= 0) totalFrames = 1;

        int frameIdx = 0;
        if (totalFrames > 1) {
            // Chỉnh tốc độ hoạt ảnh theo trạng thái
            float animSpeed = (state.equals("run")) ? FRAME_DURATION * 0.6f : FRAME_DURATION;
            if ("elephant".equals(species)) {
                animSpeed *= 1.5f;
            }
            frameIdx = (int) (animationTimer / animSpeed) % totalFrames;
        }

        int drawSize = (int) (animal.getSize() * zoom);

        int dstX1 = (int) screenPos.x - drawSize / 2;
        int dstY1 = (int) screenPos.y - drawSize / 2;
        int dstX2 = dstX1 + drawSize;
        int dstY2 = dstY1 + drawSize;

        boolean shouldFlip = animal.isFacingRight() != animal.isSpriteFacingRightByDefault();
        if (animal instanceof model.living_beings.Fish) {
            shouldFlip = !animal.isFacingRight();
        }

        if (shouldFlip) {
            int temp = dstX1;
            dstX1 = dstX2;
            dstX2 = temp;
        }

        int srcX1 = (frameIdx % cols) * frameW;
        int srcY1 = (frameIdx / cols) * frameH;
        int srcX2 = srcX1 + frameW;
        int srcY2 = srcY1 + frameH;

        // [MỚI] Procedural Animation (Hoạt ảnh bằng Code) cho các loài Cá
        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
        if (animal instanceof model.living_beings.Fish && moving) {
            // Hàm Sin tạo dao động tuần hoàn. Giảm biên độ xuống khoảng 5 độ (Math.toRadians(5))
            double swimWobble = Math.sin(animationTimer * 15.0) * Math.toRadians(5);
            g2d.rotate(swimWobble, screenPos.x, screenPos.y);
        }

        java.awt.Composite originalComposite = g2d.getComposite();
        if (animal instanceof model.living_beings.Fish) {
            // Giảm độ trong suốt (Alpha) xuống 60% để cá lặn dưới nước,
            // giúp màu xanh của nước hòa quyện vào thân cá tạo hiệu ứng chân thực
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.65f));
        }

        g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);

        if (animal instanceof model.living_beings.Fish) {
            g2d.setComposite(originalComposite);
        }

        // Khôi phục lại transform ban đầu
        g2d.setTransform(oldTransform);
    }

    private BufferedImage getEatDrinkSheet(String species) {
        if ("wolf".equals(species)) {
            return assetMap.get("wolf_west");
        }

        BufferedImage sheet = assetMap.get(species + "_eat");
        if (sheet == null) sheet = assetMap.get(species + "_drink");
        if (sheet == null) sheet = assetMap.get(species + "_west");
        return sheet;
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

    // Getters and setters for settings toggles and selection
    public boolean isShowHungerBar() { return showHungerBar; }
    public void setShowHungerBar(boolean showHungerBar) { this.showHungerBar = showHungerBar; }

    public boolean isShowThirstBar() { return showThirstBar; }
    public void setShowThirstBar(boolean showThirstBar) { this.showThirstBar = showThirstBar; }

    public boolean isShowMiniMap() { return showMiniMap; }
    public void setShowMiniMap(boolean showMiniMap) { this.showMiniMap = showMiniMap; }

    public boolean isShowSpeciesName() { return showSpeciesName; }
    public void setShowSpeciesName(boolean showSpeciesName) { this.showSpeciesName = showSpeciesName; }

    public boolean isShowDebugPath() { return showDebugPath; }
    public void setShowDebugPath(boolean showDebugPath) { this.showDebugPath = showDebugPath; }

    public boolean isShowAIVision() { return showAIVision; }
    public void setShowAIVision(boolean showAIVision) { this.showAIVision = showAIVision; }

    public boolean isShowEntitiesOnMinimap() { return showEntitiesOnMinimap; }
    public void setShowEntitiesOnMinimap(boolean showEntitiesOnMinimap) { this.showEntitiesOnMinimap = showEntitiesOnMinimap; }

    public model.living_beings.Animal getSelectedAnimal() { return selectedAnimal; }
    public void setSelectedAnimal(model.living_beings.Animal selectedAnimal) { this.selectedAnimal = selectedAnimal; }

}

package view.systems.render;

import view.systems.Camera;
import view.systems.MinimalRenderer;
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
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List; // Thêm import List
import java.util.Map;
import model.strategies.IStrategy;

public class RenderSystem {

    private Camera camera;
    private DisplayMode displayMode;
    private AssetManager assetManager;
    private float animationTimer = 0;
    private final GameConfig config = GameConfig.getInstance();
    private final float FRAME_DURATION = config.ANIMATION_FRAME_DURATION;
    private MinimalRenderer minimalRenderer;
    private TileMapRenderer tileMapRenderer;
    private EntityLayerManager layerManager;
    private EnvironmentOverlayRenderer overlayRenderer;
    private EntitySpriteRenderer spriteRenderer;
    private DebugVisualsRenderer debugVisualsRenderer;

    private GameMap gameMap;
    private final float STATUS_BAR_MIN_ZOOM = config.STATUS_BAR_MIN_ZOOM;

    private boolean showHungerBar = true;
    private boolean showThirstBar = true;
    private boolean showHealthBar = true;
    private boolean showMiniMap = true;
    private boolean showSpeciesName = false;
    private boolean showDebugPath = false;
    public boolean showStrategyLabelAll = false;
    private boolean showAIVision = false;
    private boolean showEntitiesOnMinimap = false;
    private model.entity.Entity selectedEntity = null;

    public RenderSystem(Camera camera) {
        this.camera = camera;
        this.displayMode = DisplayMode.REALISTIC;
        this.assetManager = new AssetManager();
        this.minimalRenderer = new MinimalRenderer(camera);
        this.tileMapRenderer = new TileMapRenderer();
        this.layerManager = new EntityLayerManager();
        this.overlayRenderer = new EnvironmentOverlayRenderer();
        this.spriteRenderer = new EntitySpriteRenderer(this.assetManager, this.overlayRenderer);
        this.debugVisualsRenderer = new DebugVisualsRenderer(this.overlayRenderer);
    }

    public void setGameMap(GameMap map) {
        this.gameMap = map;
        overlayRenderer.clearCache();
        debugVisualsRenderer.clearCaches();
        debugVisualsRenderer.rebuildMiniMapCache(map);
    }



    public void renderAll(World world, Graphics2D g2d, float deltaTime) {
        animationTimer += deltaTime;

        // Luôn luôn vẽ map dù ở chế độ REALISTIC hay MINIMAL
        tileMapRenderer.render(g2d, gameMap, camera);

        // [MỚI] Vẽ lớp tuyết nếu đang vào mùa đông
        overlayRenderer.renderSnow(world, gameMap, camera, g2d);

        // =========================================================
        // [MỚI] TỐI ƯU HÓA VẼ THỰC THỂ BẰNG SPATIAL GRID VÀ LAYERS
        // =========================================================
        layerManager.categorizeAndSort(world, camera, g2d.getClipBounds());
        layerManager.renderLayers(g2d, this::renderEntity);

        List<Entity> entitiesToRender = layerManager.getEntitiesToRender();
        overlayRenderer.renderNightOverlay(world, camera, g2d, entitiesToRender);

        debugVisualsRenderer.renderMiniMap(world, g2d, gameMap, camera, this);
    }

    public void rebuildMiniMapCache() {
        debugVisualsRenderer.rebuildMiniMapCache(this.gameMap);
    }


    private void renderEntity(Entity e, Graphics2D g2d) {
        if (displayMode == DisplayMode.REALISTIC) {
            Vector2 screenPos = camera.worldToScreen(e.getPosition());
            float zoom = camera.getZoomLevel();

            if (e instanceof model.living_beings.animal.Animal) {
                model.living_beings.animal.Animal animal = (model.living_beings.animal.Animal) e;
                if (animal.isHidden()) return;

                spriteRenderer.renderAnimalSpriteAndUI(animal, g2d, screenPos, zoom, animationTimer, this, camera);
            } else if (e instanceof model.items.FireballProjectile) {
                spriteRenderer.drawHunterProjectile((model.items.FireballProjectile) e, g2d, screenPos, zoom);
            } else if (e instanceof model.structures.Lantern) {
                spriteRenderer.drawLantern((model.structures.Lantern) e, g2d, screenPos, zoom);
            } else {
                BufferedImage img = null;
                String variant = e.getImageVariant();
                
                // Mùa đông đổi variant của cây
                if (e instanceof model.plants.FruitTree
                        && e.getWorld() != null
                        && e.getWorld().getWinterProgress() >= config.WINTER_TREE_SPRITE_THRESHOLD) {
                    // Tree_1 đến Tree_6 dùng winter_1, còn lại dùng winter_2 cho đa dạng
                    if (variant.matches("(?i)Tree_[1-6]")) {
                        variant = "tree_winter_1";
                    } else {
                        variant = "tree_winter_2";
                    }
                }

                if (variant != null && !variant.isEmpty()) {
                    img = assetManager.getAsset(variant.toLowerCase());
                }

                if (img != null) {
                    float aspect = (float) img.getHeight() / img.getWidth();
                    int w = (int) (e.getSize() * zoom);
                    int h = (int) (w * aspect);
                    
                    java.awt.Composite originalComposite = g2d.getComposite();
                    if (e instanceof model.plants.Seaweed) {
                        g2d.setComposite(java.awt.AlphaComposite.getInstance(
                                java.awt.AlphaComposite.SRC_OVER, 0.65f));
                    }

                    g2d.drawImage(img, (int)screenPos.x - w / 2,
                            (int) screenPos.y - h / 2, w, h, null);

                    if (e instanceof model.plants.Seaweed) {
                        g2d.setComposite(originalComposite);
                    }

                    // Nếu là Thuyền và đang thả lưới
                    if (e instanceof model.structures.Boat) {
                        model.structures.Boat boat = (model.structures.Boat) e;
                        if (boat.getState() == model.structures.Boat.BoatState.FISHING) {
                            BufferedImage net = assetManager.getAsset("fishing_net");
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

    public boolean isShowHealthBar() { return showHealthBar; }
    public void setShowHealthBar(boolean showHealthBar) { this.showHealthBar = showHealthBar; }

    public model.entity.Entity getSelectedEntity() { return selectedEntity; }
    public void setSelectedEntity(model.entity.Entity selectedEntity) { this.selectedEntity = selectedEntity; }
}

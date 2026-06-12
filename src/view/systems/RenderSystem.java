package view.systems;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;
import model.world.World;
import model.map.GameMap;
import map.TileMapRenderer;

import java.awt.*;
import java.util.List;

public class RenderSystem {

    private final Camera camera;
    private DisplayMode displayMode;
    private float animationTimer = 0;
    
    private final MinimalRenderer minimalRenderer;
    private final TileMapRenderer tileMapRenderer;
    private final OverlayRenderer overlayRenderer;
    private final MiniMapRenderer miniMapRenderer;
    private final EntityRenderer entityRenderer;

    private GameMap gameMap;

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
        this.minimalRenderer = new MinimalRenderer(camera);
        this.tileMapRenderer = new TileMapRenderer();
        
        // Ensure AssetManager is initialized
        AssetManager.getInstance();
        
        this.overlayRenderer = new OverlayRenderer(camera);
        this.miniMapRenderer = new MiniMapRenderer(camera, overlayRenderer);
        this.entityRenderer = new EntityRenderer(camera, minimalRenderer, overlayRenderer);
    }

    public void setGameMap(GameMap map) {
        this.gameMap = map;
        overlayRenderer.setGameMap(map);
        miniMapRenderer.rebuildMiniMapCache(map);
    }

    public void renderAll(World world, Graphics2D g2d, float deltaTime) {
        animationTimer += deltaTime;

        tileMapRenderer.render(g2d, gameMap, camera);

        overlayRenderer.renderSnow(world, gameMap, g2d);

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
                    groundLayer.add(e);
                } else if (e instanceof model.living_beings.Animal) {
                    model.living_beings.Animal a = (model.living_beings.Animal) e;
                    if (a.isHidden()) {
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

        for (Entity e : groundLayer) {
            entityRenderer.renderEntity(e, g2d, animationTimer, displayMode, showHungerBar, showThirstBar, showHealthBar, showSpeciesName, showDebugPath, showStrategyLabelAll, showAIVision, selectedEntity);
        }
        for (Entity e : worldLayer) {
            entityRenderer.renderEntity(e, g2d, animationTimer, displayMode, showHungerBar, showThirstBar, showHealthBar, showSpeciesName, showDebugPath, showStrategyLabelAll, showAIVision, selectedEntity);
        }
        for (Entity e : topLayer) {
            entityRenderer.renderEntity(e, g2d, animationTimer, displayMode, showHungerBar, showThirstBar, showHealthBar, showSpeciesName, showDebugPath, showStrategyLabelAll, showAIVision, selectedEntity);
        }

        overlayRenderer.renderNightOverlay(world, g2d, entitiesToRender);

        miniMapRenderer.renderMiniMap(world, gameMap, g2d, showMiniMap, showEntitiesOnMinimap);
    }

    public void rebuildMiniMapCache() {
        if (miniMapRenderer != null) {
            miniMapRenderer.rebuildMiniMapCache(gameMap);
        }
    }

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }

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

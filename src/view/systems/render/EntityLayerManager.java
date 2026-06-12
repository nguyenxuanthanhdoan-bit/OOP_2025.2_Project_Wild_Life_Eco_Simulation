package view.systems.render;

import core.Vector2;
import model.entity.Entity;
import model.world.World;
import view.systems.Camera;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

public class EntityLayerManager {

    private List<Entity> groundLayer;
    private List<Entity> worldLayer;
    private List<Entity> topLayer;
    private List<Entity> entitiesToRender;

    public EntityLayerManager() {
        this.groundLayer = new ArrayList<>();
        this.worldLayer = new ArrayList<>();
        this.topLayer = new ArrayList<>();
        this.entitiesToRender = new ArrayList<>();
    }

    public void categorizeAndSort(World world, Camera camera, Rectangle clipBounds) {
        groundLayer.clear();
        worldLayer.clear();
        topLayer.clear();
        entitiesToRender.clear();

        if (world.getSpatialGrid() != null) {
            Vector2 camPos = camera.getPosition();
            float zoom = camera.getZoomLevel();

            float screenW = (clipBounds != null) ? clipBounds.width : 800;
            float screenH = (clipBounds != null) ? clipBounds.height : 600;

            Vector2 centerView = new Vector2(
                    camPos.x + (screenW / zoom) / 2f,
                    camPos.y + (screenH / zoom) / 2f
            );

            float scanRange = (Math.max(screenW, screenH) / zoom) / 2f + 100f;
            entitiesToRender = world.getSpatialGrid().getNeighbors(centerView, scanRange);
        } else {
            entitiesToRender.addAll(world.getEntities());
        }

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
                } else if (e instanceof model.living_beings.animal.Animal) {
                    model.living_beings.animal.Animal a = (model.living_beings.animal.Animal) e;
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

        worldLayer.sort(Comparator.comparingDouble(e -> e.getPosition().y + e.getSize() * 0.5f));
    }

    public void renderLayers(Graphics2D g2d, BiConsumer<Entity, Graphics2D> renderFunc) {
        for (Entity e : groundLayer) renderFunc.accept(e, g2d);
        for (Entity e : worldLayer) renderFunc.accept(e, g2d);
        for (Entity e : topLayer) renderFunc.accept(e, g2d);
    }

    public List<Entity> getEntitiesToRender() {
        return entitiesToRender;
    }
}

package controller;

import model.world.World;
import model.map.GameMap; 
import view.systems.RenderSystem;
import view.systems.Camera;
import core.DisplayMode;
import core.GameConfig;
import java.awt.Graphics2D;
import core.Vector2;

/**
 * Trái tim điều phối toàn bộ hệ thống.
 */
public class Simulation {

    private World world;
    private RenderSystem renderSystem;
    private Camera camera;
    private DisplayMode currentDisplayMode;

    private GameMap gameMap;

    public Simulation(Camera camera, World world, RenderSystem renderSystem) {
        this.camera = camera;
        this.world = world;
        this.renderSystem = renderSystem;
        this.currentDisplayMode = DisplayMode.REALISTIC;

        initMap();
        spawnInitialEntities();
    }

    private void initMap() {
        this.gameMap = new GameMap(GameConfig.getInstance().DEFAULT_MAP_PATH);
        this.renderSystem.setGameMap(this.gameMap);
        this.world.setGameMap(this.gameMap); 

        float realWorldWidth = gameMap.getCols() * 32f;
        float realWorldHeight = gameMap.getRows() * 32f;
        this.world.setWidth(realWorldWidth);
        this.world.setHeight(realWorldHeight);
        this.camera.setWorldBounds(realWorldWidth, realWorldHeight);
        camera.setPosition(new Vector2(realWorldWidth / 2, realWorldHeight / 2));
    }


    private void spawnInitialEntities() {
        model.world.BiomeGenerator.generateBiomes(this.world, this.gameMap);
    }

    public void update(float deltaTime) {
        world.update(deltaTime);
    }

    public void render(Graphics2D g2d, float deltaTime) {
        renderSystem.renderAll(world, g2d, deltaTime);
    }

    public void toggleDisplayMode() {
        if (currentDisplayMode == DisplayMode.REALISTIC) {
            currentDisplayMode = DisplayMode.MINIMAL;
        } else {
            currentDisplayMode = DisplayMode.REALISTIC;
        }
        renderSystem.setDisplayMode(currentDisplayMode);
    }

    public void spawnCarcassAtCameraCenter() {
        Vector2 pos = camera.getPosition().copy();
        // Giả sử màn hình 800x600, cộng một chút offset để nó vào khoảng giữa
        pos.x += 400 / camera.getZoomLevel();
        pos.y += 300 / camera.getZoomLevel();
        model.items.Carcass carcass = new model.items.Carcass(pos, 30.0f, 100.0f, 120.0f, 100.0f, "Spawn");
        world.addEntity(carcass);
    }
}

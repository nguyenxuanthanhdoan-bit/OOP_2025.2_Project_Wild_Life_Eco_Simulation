package controller;

import model.world.World;
import model.map.GameMap; 
import view.systems.RenderSystem;
import view.systems.Camera;
import core.DisplayMode;
import java.awt.Graphics2D;
import core.Vector2;
import model.plants.Grass;
import model.plants.FruitTree;
import model.living_beings.Rabbit;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import java.util.Random;
import java.awt.geom.Rectangle2D;
import model.map.GameMap.MapPolygonObject;
import java.util.List;
import java.util.ArrayList;

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
        this.gameMap = new GameMap("resources/map/map2.tmx");
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
}
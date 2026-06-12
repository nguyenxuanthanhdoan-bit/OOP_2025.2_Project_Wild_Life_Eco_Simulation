package model.world;

import core.GameConfig;
import core.Vector2;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;
import model.plants.FruitTree;
import model.plants.Grass;
import model.plants.Mushroom;
import model.structures.Bush;
import model.structures.Rock;

import java.util.List;
import java.util.Random;

public class VegetationSpawner {

    public static void spawnVegetation(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, List<MapPolygonObject> village, Random rand) {
        GameConfig config = GameConfig.getInstance();

        for (int i = 0; i < config.INITIAL_GRASS_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.GRASS_PLAIN_SPAWN_CHANCE;
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Grass(pos));
        }

        for (int i = 0; i < config.INITIAL_MUSHROOM_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.MUSHROOM_PLAIN_SPAWN_CHANCE;
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Mushroom(pos));
        }

        int[] normalTrees = {1, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        
        for (int i = 0; i < config.INITIAL_PLAIN_TREE_COUNT; i++) {
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(plain, gameMap, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }

        for (int i = 0; i < config.INITIAL_FOREST_TREE_COUNT; i++) {
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(forest, gameMap, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }

        int desertVegetationSpawned = 0;
        int maxAttempts = 1500;
        while (desertVegetationSpawned < 40 && maxAttempts > 0) {
            maxAttempts--;
            float x = rand.nextFloat() * world.getWidth();
            float y = rand.nextFloat() * world.getHeight();
            if (gameMap != null && gameMap.isSandTile(x, y)) {
                Vector2 pos = new Vector2(x, y);
                if (rand.nextFloat() < 0.6f) {
                    world.addEntity(new model.plants.Cactus(pos));
                } else {
                    world.addEntity(new model.plants.Straw(pos));
                }
                desertVegetationSpawned++;
            }
        }
    }

    public static void spawnStructures(World world, GameMap gameMap, List<MapPolygonObject> plain, List<MapPolygonObject> forest, Random rand) {
        GameConfig config = GameConfig.getInstance();

        for (int i = 0; i < config.INITIAL_BUSH_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.BUSH_PLAIN_SPAWN_CHANCE;
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Bush(pos));
        }

        for (int i = 0; i < config.INITIAL_ROCK_COUNT; i++) {
            boolean inPlain = rand.nextFloat() < config.ROCK_PLAIN_SPAWN_CHANCE;
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(inPlain ? plain : forest, gameMap, rand);
            if (pos != null) world.addEntity(new Rock(pos));
        }
    }
}

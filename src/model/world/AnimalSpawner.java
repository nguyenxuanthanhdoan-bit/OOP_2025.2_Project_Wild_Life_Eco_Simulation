package model.world;

import core.GameConfig;
import core.Vector2;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Rabbit;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.map.GameMap;
import model.map.GameMap.MapPolygonObject;

import java.util.List;
import java.util.Random;

public class AnimalSpawner {

    public static void spawnAnimals(World world, GameMap gameMap, List<MapPolygonObject> plain,
                                     List<MapPolygonObject> forest, List<MapPolygonObject> excludedZones, Random rand) {
        spawnSpecies(world, gameMap, plain, excludedZones, 45, 3, rand, (pos, index) -> new Rabbit(pos));
        spawnSpecies(world, gameMap, forest, excludedZones, 25, 2, rand, (pos, index) -> new Rabbit(pos));

        spawnSpecies(world, gameMap, plain, excludedZones, 25, 3, rand, (pos, index) -> new Deer(pos, 1 + index / 16));
        spawnSpecies(world, gameMap, forest, excludedZones, 15, 2, rand, (pos, index) -> new Deer(pos, 3 + index / 12));

        spawnSpecies(world, gameMap, plain, excludedZones, 1, 1, rand, (pos, index) -> new Elephant(pos, 1));
        spawnSpecies(world, gameMap, forest, excludedZones, 1, 1, rand, (pos, index) -> new Elephant(pos, 2 + index / 6));

        spawnSpecies(world, gameMap, plain, excludedZones, 1, 0, rand, (pos, index) -> new Tiger(pos));
        spawnSpecies(world, gameMap, forest, excludedZones, 1, 1, rand, (pos, index) -> new Tiger(pos));

        spawnSpecies(world, gameMap, plain, excludedZones, 6, 1, rand, (pos, index) -> new Wolf(pos));
        spawnSpecies(world, gameMap, forest, excludedZones, 8, 2, rand, (pos, index) -> new Wolf(pos));

        int foxCountToSpawn = 3 + rand.nextInt(3);
        int foxesSpawned = 0;
        int maxAttempts = 1500;
        while (foxesSpawned < foxCountToSpawn && maxAttempts > 0) {
            maxAttempts--;
            float x = rand.nextFloat() * world.getWidth();
            float y = rand.nextFloat() * world.getHeight();
            if (gameMap == null || !gameMap.isSandTile(x, y)) continue;
            if (SpawnHelper.isInsideAnyPolygon(excludedZones, x, y)) continue;

            Vector2 pos = new Vector2(x, y);
            model.living_beings.Fox fox = new model.living_beings.Fox(pos);
            if (!world.isValidPositionFor(fox, pos)
                    || !SpawnHelper.hasVillageResidentClearance(world, fox, pos, 1.0f)) {
                continue;
            }

            addSpawnedAnimal(world, fox, rand);
            foxesSpawned++;
        }

        spawnMapWideWildlife(world, gameMap, excludedZones, rand, GameConfig.getInstance().MAX_INITIAL_ANIMAL_COUNT);
    }

    private static void spawnSpecies(World world, GameMap gameMap, List<MapPolygonObject> zones,
                                     List<MapPolygonObject> excludedZones,
                                     int totalCount, int minPerZone, Random rand, SpawnHelper.AnimalFactory factory) {
        if (zones == null || zones.isEmpty() || totalCount <= 0) return;

        int spawned = 0;
        int speciesIndex = 0;
        int guaranteedPerZone = Math.min(minPerZone, Math.max(0, totalCount / zones.size()));

        for (MapPolygonObject zone : zones) {
            for (int i = 0; i < guaranteedPerZone && spawned < totalCount; i++) {
                Vector2 pos = SpawnHelper.getRandomPointInPolygon(zone, gameMap, rand, excludedZones);
                if (pos != null) {
                    addSpawnedAnimal(world, factory.create(pos, speciesIndex++), rand);
                    spawned++;
                }
            }
        }

        while (spawned < totalCount) {
            Vector2 pos = SpawnHelper.getRandomPointInPolygons(zones, gameMap, rand, excludedZones);
            if (pos == null) break;
            addSpawnedAnimal(world, factory.create(pos, speciesIndex++), rand);
            spawned++;
        }
    }

    public static void addSpawnedAnimal(World world, Animal animal, Random rand) {
        GameConfig config = GameConfig.getInstance();
        double ageRatio = config.INITIAL_SPAWN_MIN_AGE_RATIO
                + rand.nextDouble() * (config.INITIAL_SPAWN_MAX_AGE_RATIO - config.INITIAL_SPAWN_MIN_AGE_RATIO);
        animal.setAge(animal.getMaxAge() * ageRatio);
        animal.setAdult(true);
        world.addEntity(animal);
    }

    private static void spawnMapWideWildlife(World world, GameMap gameMap, List<MapPolygonObject> excludedZones,
                                             Random rand, int targetAnimalCount) {
        GameConfig config = GameConfig.getInstance();
        int attempts = 0;
        int maxAttempts = targetAnimalCount * config.SUPPLEMENTAL_SPAWN_ATTEMPT_MULTIPLIER;
        while (countAnimals(world) < targetAnimalCount && attempts < maxAttempts) {
            attempts++;
            Vector2 pos = SpawnHelper.getRandomGroundPoint(world, gameMap, excludedZones, rand);
            if (pos == null) break;
            Animal animal = createSupplementalAnimal(pos, rand);
            if (animal != null) {
                addSpawnedAnimal(world, animal, rand);
            }
        }
    }

    private static Animal createSupplementalAnimal(Vector2 pos, Random rand) {
        float roll = rand.nextFloat();
        if (roll < 0.60f) return EntityFactory.createAnimal("Thỏ", pos, 0, rand);
        if (roll < 0.85f) return new Deer(pos, 10 + rand.nextInt(4));
        if (roll < 0.95f) return EntityFactory.createAnimal("Sói", pos, 0, rand);
        if (roll < 0.98f) return EntityFactory.createAnimal("Cáo", pos, 0, rand);
        if (roll < 0.99f) return EntityFactory.createAnimal("Hổ", pos, 0, rand);
        return new Elephant(pos, 20 + rand.nextInt(3));
    }

    private static int countAnimals(World world) {
        int count = 0;
        for (model.entity.Entity entity : world.getEntities()) {
            if (entity instanceof Animal && entity.isAlive()) {
                count++;
            }
        }
        return count;
    }
}

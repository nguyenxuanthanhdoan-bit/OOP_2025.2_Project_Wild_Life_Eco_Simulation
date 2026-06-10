package model.world;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Fish;
import model.living_beings.Shark;
import model.living_beings.FishFactory;
import model.map.GameMap;

import java.util.Random;

public class FishPopulationManager {
    private final World world;
    private final int TARGET_PREY_COUNT = 40;
    private final int TARGET_PREDATOR_COUNT = TARGET_PREY_COUNT / 2;
    private float checkTimer = 5.0f; // Khởi tạo bằng CHECK_INTERVAL để spawn ngay lập tức lúc mới mở game
    private final float CHECK_INTERVAL = 5.0f; // Kiểm tra mỗi 5 giây
    private final Random rand = new Random();

    public FishPopulationManager(World world) {
        this.world = world;
    }

    public void update(float deltaTime) {
        checkTimer += deltaTime;
        if (checkTimer >= CHECK_INTERVAL) {
            checkTimer = 0f;
            balancePopulation();
        }
    }

    private void balancePopulation() {
        GameMap map = world.getGameMap();
        if (map == null) return;

        int preyCount = 0;
        int predatorCount = 0;

        for (Entity e : world.getEntities()) {
            if (e instanceof Fish && e.isAlive()) {
                if (e instanceof Shark) {
                    predatorCount++;
                } else {
                    preyCount++;
                }
            }
        }

        // Bù đắp số lượng cá bị săn mồi
        int preyToSpawn = TARGET_PREY_COUNT - preyCount;
        int spawnedPrey = 0;
        for (int i = 0; i < preyToSpawn; i++) {
            if (spawnRandomPrey(map)) spawnedPrey++;
        }

        // Bù đắp số lượng cá mập
        int predatorsToSpawn = TARGET_PREDATOR_COUNT - predatorCount;
        int spawnedPredator = 0;
        for (int i = 0; i < predatorsToSpawn; i++) {
            if (spawnRandomPredator(map)) spawnedPredator++;
        }

        System.out.println("[FishPopulationManager] Checked! Prey: " + preyCount + " (Spawned " + spawnedPrey + 
                           "), Predators: " + predatorCount + " (Spawned " + spawnedPredator + ")");
    }

    private boolean spawnRandomPrey(GameMap map) {
        Vector2 pos = getRandomWaterPosition(map);
        if (pos != null) {
            String species = rand.nextBoolean() ? "Sunfish" : "Clownfish";
            Fish fish = FishFactory.create(species, pos);
            if (fish != null) {
                fish.setAge(rand.nextDouble() * fish.getMaxAge() * 0.5);
                world.addEntity(fish);
                return true;
            }
        }
        return false;
    }

    private boolean spawnRandomPredator(GameMap map) {
        Vector2 pos = getRandomWaterPosition(map);
        if (pos != null) {
            Fish shark = FishFactory.create("Shark", pos);
            if (shark != null) {
                shark.setAge(rand.nextDouble() * shark.getMaxAge() * 0.5);
                world.addEntity(shark);
                return true;
            }
        }
        return false;
    }

    private Vector2 getRandomWaterPosition(GameMap map) {
        int cols = map.getCols();
        int rows = map.getRows();
        
        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(cols);
            int y = rand.nextInt(rows);
            // 32 là kích thước tile mặc định
            if (map.isWaterTile(x * 32 + 16, y * 32 + 16)) {
                return new Vector2(x * 32 + 16, y * 32 + 16);
            }
        }
        return null;
    }
}

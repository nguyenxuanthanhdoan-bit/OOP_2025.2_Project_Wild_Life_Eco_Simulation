package model.world;

import core.Vector2;
import core.GameConfig;
import model.entity.Entity;
import model.living_beings.*;
import java.util.List;
import java.util.Random;

/**
 * Quản lý quần thể để tránh tuyệt chủng.
 */
public class PopulationManager {
    /** Cho phép bật/tắt auto-spawn (hữu ích khi test). */
    private static boolean enabled = true;
    public static void setEnabled(boolean value) { enabled = value; }

    /**
     * Kích hoạt khi một động vật chết.
     */
    public static void onAnimalDeath(Animal animal, World world) {
        if (!enabled || world == null) return;
        GameConfig config = GameConfig.getInstance();
        
        int currentPop = countSpecies(animal.getSpeciesName(), world);
        // Trừ đi 1 vì con hiện tại đang chết nhưng có thể chưa bị xóa khỏi list
        if (animal.isAliveState()) { // Tuy nhiên die() gọi sau khi set alive = false
            // Nên không cần trừ, countSpecies đã kiểm tra isAliveState()
        }

        if (currentPop < config.MIN_SPECIES_POPULATION && currentPop < config.MAX_SPECIES_POPULATION) {
            spawnAnimal(world, animal.getSpeciesName());
        }
    }

    private static int countSpecies(String speciesName, World world) {
        int count = 0;
        for (Entity e : world.getEntities()) {
            if (e instanceof Animal && e.isAlive()) {
                Animal a = (Animal) e;
                if (a.getSpeciesName().equals(speciesName)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void spawnAnimal(World world, String species) {
        if (!EntityFactory.canCreateAnimal(species)) return;

        GameConfig config = GameConfig.getInstance();
        Random rand = new Random();
        Vector2 pos = null;
        
        // Cố gắng tìm vị trí xa kẻ săn mồi
        for (int i = 0; i < config.POPULATION_RESPAWN_ATTEMPTS; i++) {
            float x = 200 + rand.nextFloat() * (world.getWidth() - 400);
            float y = 200 + rand.nextFloat() * (world.getHeight() - 400);
            
            if (!world.isValidGroundSpawnPosition(x, y, config.GROUND_SPAWN_MARGIN)) continue;
            
            boolean safe = true;
            if (world.getSpatialGrid() != null) {
                List<Entity> neighbors = world.getSpatialGrid().getNeighbors(
                        new Vector2(x, y),
                        config.POPULATION_SAFE_SPAWN_PREDATOR_RADIUS
                );
                for (Entity n : neighbors) {
                    if (n instanceof CarnivoreAnimal && n.isAlive()) {
                        safe = false; 
                        break;
                    }
                }
            }
            if (safe) {
                pos = new Vector2(x, y);
                break;
            }
        }
        
        if (pos == null) {
            pos = new Vector2(world.getWidth() / 2, world.getHeight() / 2);
        }
        
        Animal animal = EntityFactory.createAnimal(species, pos);

        if (animal != null) {
            double ageRatio = config.INITIAL_SPAWN_MIN_AGE_RATIO
                    + rand.nextDouble() * (config.INITIAL_SPAWN_MAX_AGE_RATIO - config.INITIAL_SPAWN_MIN_AGE_RATIO);
            animal.setAge(animal.getMaxAge() * ageRatio);
            animal.setAdult(true);
            world.addEntity(animal);
        }
    }
}

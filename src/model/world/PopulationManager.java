package model.world;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.*;
import java.util.List;
import java.util.Random;

/**
 * Quản lý quần thể để tránh tuyệt chủng.
 */
public class PopulationManager {
    public static final int MIN_SPECIES_POPULATION = 15;
    public static final int MAX_SPECIES_POPULATION = 100;

    /**
     * Kích hoạt khi một động vật chết.
     */
    public static void onAnimalDeath(Animal animal, World world) {
        if (world == null) return;
        
        int currentPop = countSpecies(animal.getSpeciesName(), world);
        // Trừ đi 1 vì con hiện tại đang chết nhưng có thể chưa bị xóa khỏi list
        if (animal.isAliveState()) { // Tuy nhiên die() gọi sau khi set alive = false
            // Nên không cần trừ, countSpecies đã kiểm tra isAliveState()
        }

        if (currentPop < MIN_SPECIES_POPULATION) {
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
        Random rand = new Random();
        Vector2 pos = null;
        
        // Cố gắng tìm vị trí xa kẻ săn mồi
        for (int i = 0; i < 30; i++) {
            float x = 200 + rand.nextFloat() * (world.getWidth() - 400);
            float y = 200 + rand.nextFloat() * (world.getHeight() - 400);
            
            if (world.isPositionInWater(x, y)) continue;
            
            boolean safe = true;
            if (world.getSpatialGrid() != null) {
                List<Entity> neighbors = world.getSpatialGrid().getNeighbors(new Vector2(x, y), 500.0f);
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
        
        switch (species) {
            case "Thỏ": world.addEntity(new Rabbit(pos)); break;
            case "Hươu": world.addEntity(new Deer(pos)); break;
            case "Voi": world.addEntity(new Elephant(pos)); break;
            case "Sói": world.addEntity(new Wolf(pos)); break;
            case "Hổ": world.addEntity(new Tiger(pos)); break;
        }
    }
}

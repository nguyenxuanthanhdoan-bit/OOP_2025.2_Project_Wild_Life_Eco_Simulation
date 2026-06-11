package model.plants;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;

import java.util.List;
import java.util.Random;

public class Grass extends Plant {
    private static final float SPAWN_INTERVAL = 12.0f;
    private static final float SPAWN_INTERVAL_VARIANCE = 6.0f;
    private static final float SPAWN_RADIUS = 110.0f;
    private static final int MAX_GRASS_NEARBY = 9;

    private float spawnTimer;
    private Random random;

    public Grass(Vector2 position) {
        super(position, 15.0f, 30.0f); // Cỏ: 10 → 30
        this.isSolid = false; // Cỏ có thể dẫm lên (không cản đường)
        
        // Random hình ảnh từ Grass_1 đến Grass_2
        this.random = new Random();
        int variant = random.nextInt(2) + 1;
        this.imageVariant = "Grass_" + variant;
        this.spawnTimer = randomSpawnDelay();
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive || world == null) return;
        
        float currentDelta = deltaTime;
        float currentSpawnRadius = SPAWN_RADIUS;
        int currentMaxNearby = MAX_GRASS_NEARBY;

        if (world.getCurrentSeason() == model.world.World.Season.GROWING) {
            currentDelta *= 2.0f;
            currentSpawnRadius *= 1.5f;
            currentMaxNearby *= 2;
        } else if (world.getCurrentSeason() == model.world.World.Season.WINTER) {
            currentDelta *= 0.3f;
        }

        spawnTimer -= currentDelta;
        if (spawnTimer <= 0) {
            spawnTimer = randomSpawnDelay();
            
            if (world.getSpatialGrid() != null) {
                List<Entity> nearby = world.getSpatialGrid().getNeighbors(position, currentSpawnRadius);
                int grassCount = 0;
                for (Entity e : nearby) {
                    if (e instanceof Grass && e.isAlive()) {
                        grassCount++;
                    }
                }
                
                if (grassCount < currentMaxNearby) {
                    float angle = random.nextFloat() * 2 * (float)Math.PI;
                    float dist = 20.0f + random.nextFloat() * (currentSpawnRadius - 20.0f);
                    float dx = (float)Math.cos(angle) * dist;
                    float dy = (float)Math.sin(angle) * dist;
                    
                    Vector2 newPos = new Vector2(position.x + dx, position.y + dy);
                    
                    if (newPos.x > 0 && newPos.x < world.getWidth() &&
                        newPos.y > 0 && newPos.y < world.getHeight()) {
                        
                        if (!world.isPositionInWater(newPos.x, newPos.y)) {
                            Grass newGrass = new Grass(newPos);
                            world.addEntity(newGrass);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Grass_1" hoặc "Grass_2") để vẽ hình
    }

    private float randomSpawnDelay() {
        return SPAWN_INTERVAL + random.nextFloat() * SPAWN_INTERVAL_VARIANCE;
    }
}

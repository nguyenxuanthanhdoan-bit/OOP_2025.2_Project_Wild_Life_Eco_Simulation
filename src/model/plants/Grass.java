package model.plants;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;

import java.util.List;
import java.util.Random;

public class Grass extends Plant {
    private static final float SPAWN_INTERVAL = 25.0f; // Mỗi 25s sinh sản
    private static final float SPAWN_RADIUS = 80.0f;
    private static final int MAX_GRASS_NEARBY = 5;

    private float spawnTimer;
    private Random random;

    public Grass(Vector2 position) {
        super(position, 15.0f, 10.0f);
        this.isSolid = false; // Cỏ có thể dẫm lên (không cản đường)
        
        // Random hình ảnh từ Grass_1 đến Grass_2
        this.random = new Random();
        int variant = random.nextInt(2) + 1;
        this.imageVariant = "Grass_" + variant;
        this.spawnTimer = SPAWN_INTERVAL + random.nextFloat() * 10;
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive || world == null) return;
        
        spawnTimer -= deltaTime;
        if (spawnTimer <= 0) {
            spawnTimer = SPAWN_INTERVAL + random.nextFloat() * 10;
            
            if (world.getSpatialGrid() != null) {
                List<Entity> nearby = world.getSpatialGrid().getNeighbors(position, SPAWN_RADIUS);
                int grassCount = 0;
                for (Entity e : nearby) {
                    if (e instanceof Grass && e.isAlive()) {
                        grassCount++;
                    }
                }
                
                if (grassCount < MAX_GRASS_NEARBY) {
                    float angle = random.nextFloat() * 2 * (float)Math.PI;
                    float dist = 20.0f + random.nextFloat() * (SPAWN_RADIUS - 20.0f);
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
}
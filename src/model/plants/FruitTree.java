package model.plants;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;

import java.util.List;
import java.util.Random;

public class FruitTree extends Plant {
    private static final float DROP_INTERVAL = 40.0f; // Mỗi 40s rơi quả
    private static final float DROP_RADIUS = 50.0f;
    private static final int MAX_FRUITS_NEARBY = 3;

    private float dropTimer;
    private Random random;

    public FruitTree(Vector2 position) {
        this(position, new Random().nextInt(13) + 1);
    }

    public FruitTree(Vector2 position, int variantId) {
        // Cây không bị ăn nên dinh dưỡng = 0, kích thước mặc định 60.0f
        super(position, 60.0f, 0);
        this.isSolid = true; // Là vật cản
        this.collider = new model.collision.Collider(this, 15.0f, model.collision.CollisionLayer.OBSTACLE); // Gốc cây nhỏ
        this.imageVariant = "Tree_" + variantId;
        this.random = new Random();
        this.dropTimer = DROP_INTERVAL + random.nextFloat() * 10;
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive || world == null) return;
        
        float currentDelta = deltaTime;
        if (world.getCurrentSeason() == model.world.World.Season.GROWING) {
            currentDelta *= 1.5f; // Rớt quả nhanh hơn 1.5 lần
        } else if (world.getCurrentSeason() == model.world.World.Season.WINTER) {
            currentDelta *= (1.0f - world.getWinterProgress() * 0.8f); // Chậm tối đa 80%
        }
        
        dropTimer -= currentDelta;
        if (dropTimer <= 0) {
            dropTimer = DROP_INTERVAL + random.nextFloat() * 10;
            
            if (world.getSpatialGrid() != null) {
                List<Entity> nearby = world.getSpatialGrid().getNeighbors(position, DROP_RADIUS);
                int fruitCount = 0;
                for (Entity e : nearby) {
                    if (e instanceof Fruit && e.isAlive()) {
                        fruitCount++;
                    }
                }
                
                if (fruitCount < MAX_FRUITS_NEARBY) {
                    float angle = random.nextFloat() * 2 * (float)Math.PI;
                    float dist = 20.0f + random.nextFloat() * (DROP_RADIUS - 20.0f);
                    float dx = (float)Math.cos(angle) * dist;
                    float dy = (float)Math.sin(angle) * dist;
                    
                    Vector2 newPos = new Vector2(position.x + dx, position.y + dy);
                    
                    if (newPos.x > 0 && newPos.x < world.getWidth() &&
                        newPos.y > 0 && newPos.y < world.getHeight()) {
                        
                        if (!world.isPositionInWater(newPos.x, newPos.y)) {
                            Fruit newFruit = new Fruit(newPos);
                            world.addEntity(newFruit);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng this.imageVariant ("Tree_1", ..., "Tree_5") để vẽ hình
    }
}
package model.plants;

import core.Vector2;
import core.DisplayMode;
import model.entity.Entity;

import java.util.List;
import java.util.Random;

public class FruitTree extends Plant {
    private static final float DROP_INTERVAL = 15.0f; // Mỗi 15s rơi quả thay vì 40s
    private static final float DROP_RADIUS = 120.0f;
    private static final int MAX_FRUITS_NEARBY = 3;

    private float dropTimer;
    private Random random;

    public FruitTree(Vector2 position) {
        this(position, new Random().nextInt(13) + 1);
    }

    public FruitTree(Vector2 position, int variantId) {
        // Cây không bị ăn nên dinh dưỡng = 0, tăng kích thước lên 90.0f để thành vật cản cứng và to hơn
        super(position, 90.0f, 0);
        this.isSolid = true; // Là vật cản
        this.imageVariant = "Tree_" + variantId;
        this.random = new Random();
        // Cho rớt quả rất nhanh ngay khi vừa vào game (chỉ từ 0 - 5 giây đầu)
        this.dropTimer = random.nextFloat() * 5.0f;
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive || world == null) return;
        
        dropTimer -= deltaTime;
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
                    // Bán kính cây là 45 (size 90/2). Quả phải rớt từ khoảng cách 60 -> 110 để không bị kẹt vào cây
                    float dist = 60.0f + random.nextFloat() * (DROP_RADIUS - 60.0f);
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
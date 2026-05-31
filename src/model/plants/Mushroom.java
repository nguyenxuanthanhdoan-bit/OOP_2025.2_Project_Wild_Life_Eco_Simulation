package model.plants;

import core.DisplayMode;
import core.Vector2;
import model.entity.Entity;

import java.util.List;
import java.util.Random;

public class Mushroom extends Plant {
    private static final float MUSHROOM_NUTRITION = 5.0f;
    private static final float MUSHROOM_SIZE = 10.0f;
    private static final float SPAWN_INTERVAL = 30.0f; // Mỗi 30s đẻ 1 lần
    private static final float SPAWN_RADIUS = 100.0f;
    private static final int MAX_MUSHROOMS_NEARBY = 4; // Tối đa 4 nấm lân cận

    private float spawnTimer;
    private Random random;

    public Mushroom(Vector2 position) {
        super(position, MUSHROOM_SIZE, MUSHROOM_NUTRITION);
        this.isSolid = false;
        this.random = new Random();
        this.imageVariant = "Mushroom_" + (random.nextInt(8) + 1);
        this.spawnTimer = SPAWN_INTERVAL + random.nextFloat() * 10; // Đặt lệch timer để không spawn cùng lúc
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive || world == null) return;
        
        spawnTimer -= deltaTime;
        if (spawnTimer <= 0) {
            spawnTimer = SPAWN_INTERVAL + random.nextFloat() * 10;
            
            // Tìm số lượng Mushroom xung quanh
            if (world.getSpatialGrid() != null) {
                List<Entity> nearby = world.getSpatialGrid().getNeighbors(position, SPAWN_RADIUS);
                int mushroomCount = 0;
                for (Entity e : nearby) {
                    if (e instanceof Mushroom && e.isAlive()) {
                        mushroomCount++;
                    }
                }
                
                // Nếu chưa đạt tối đa, sinh ra 1 nấm mới
                if (mushroomCount < MAX_MUSHROOMS_NEARBY) {
                    float angle = random.nextFloat() * 2 * (float)Math.PI;
                    float dist = 20.0f + random.nextFloat() * (SPAWN_RADIUS - 20.0f);
                    float dx = (float)Math.cos(angle) * dist;
                    float dy = (float)Math.sin(angle) * dist;
                    
                    Vector2 newPos = new Vector2(position.x + dx, position.y + dy);
                    
                    // Nấm thường xuất hiện trong cỏ hoặc vùng ẩm ướt, kiểm tra giới hạn map
                    if (newPos.x > 0 && newPos.x < world.getWidth() &&
                        newPos.y > 0 && newPos.y < world.getHeight()) {
                        
                        // Không bắt buộc phải check isPositionInWater vì đôi khi mọc ven hồ cũng tốt,
                        // nhưng không nên mọc trôi nổi giữa hồ nước sâu. Giả sử cứ tạo ra, hoặc kiểm tra không mọc dưới nước.
                        if (!world.isPositionInWater(newPos.x, newPos.y)) {
                            Mushroom newMushroom = new Mushroom(newPos);
                            world.addEntity(newMushroom);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ xử lý
    }
}

package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.world.World;
import java.util.Random;

public class PassiveStrategy implements IStrategy {
    private Vector2 wanderDirection = new Vector2();
    private float wanderAngle = 0;
    private float stateTimer;
    private boolean isIdling;
    private Random random = new Random();

    public PassiveStrategy() {
        wanderAngle = (float) (random.nextFloat() * Math.PI * 2);
    }

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        stateTimer -= deltaTime;
        if (stateTimer <= 0) {
            isIdling = random.nextBoolean();
            if (isIdling) {
                stateTimer = 1.0f + random.nextFloat();
            } else {
                stateTimer = 2.0f + random.nextFloat() * 2.0f;
            }
        }

        Vector2 finalDir = new Vector2();
        
        if (!isIdling) {
            // Smart Wander: thay đổi hướng từ từ (jitter nhỏ)
            float jitter = 0.5f; // Góc lệch tối đa mỗi lần chuyển
            wanderAngle += (random.nextFloat() * 2 - 1) * jitter;
            
            wanderDirection.set((float) Math.cos(wanderAngle), (float) Math.sin(wanderAngle));
            finalDir.add(wanderDirection);
        }

        // Tích hợp các lực đẩy
        Vector2 separation = SteeringBehavior.calculateSeparation(owner, world);
        Vector2 boundary = SteeringBehavior.calculateBoundaryAvoidance(owner, world);
        Vector2 obstacle = SteeringBehavior.calculateObstacleAvoidance(owner, world, finalDir);

        if (owner instanceof model.living_beings.Animal) {
            Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce((model.living_beings.Animal) owner, world);
            finalDir.add(avoidance);
        }

        // Thêm các lực vật lý
        finalDir.add(separation.scale(1.5f));
        finalDir.add(boundary.scale(2.0f));
        finalDir.add(obstacle.scale(2.5f)); // Lực né vật cản rất mạnh

        if (finalDir.lengthSquared() > 0) {
            finalDir.normalize();
            if (finalDir.x > 0.1f) owner.setFacingRight(true);
            else if (finalDir.x < -0.1f) owner.setFacingRight(false);
            
            // Cập nhật lại wanderAngle nếu bị đẩy bởi lực ngoài
            if (separation.lengthSquared() > 0 || boundary.lengthSquared() > 0 || obstacle.lengthSquared() > 0) {
                wanderAngle = (float) Math.atan2(finalDir.y, finalDir.x);
            }
            
            owner.move(finalDir, deltaTime);
        }
    }

    public void forceStateChange() {
        // Chỉ đổi góc 90-180 độ chứ không reset random hoàn toàn
        float turnAngle = (float) (Math.PI / 2 + random.nextFloat() * Math.PI);
        wanderAngle += turnAngle;
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getName() {
        return "Passive";
    }
}
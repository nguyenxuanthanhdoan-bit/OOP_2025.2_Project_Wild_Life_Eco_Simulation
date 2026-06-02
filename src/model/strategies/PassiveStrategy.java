package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.world.World;
import java.util.Random;

public class PassiveStrategy implements IStrategy {
    private Vector2 wanderDirection = new Vector2();
    private float stateTimer;
    private boolean isIdling;
    private Random random = new Random();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        stateTimer -= deltaTime;
        if (stateTimer <= 0) {
            // 50% đứng nghỉ, 50% đi dạo
            isIdling = random.nextBoolean();
            if (isIdling) {
                stateTimer = 1.0f + random.nextFloat();
            } else {
                float dx = (random.nextFloat() * 2) - 1;
                float dy = (random.nextFloat() * 2) - 1;
                wanderDirection.set(dx, dy).normalize();
                stateTimer = 2.0f + random.nextFloat() * 2.0f;

                // Cập nhật hướng mặt ngay khi có hướng đi mới
                if (dx > 0) {
                    owner.setFacingRight(true);  // Đi sang phải thì quay mặt sang phải
                } else if (dx < 0) {
                    owner.setFacingRight(false); // Đi sang trái thì lật mặt lại
                }
                // --- KẾT THÚC FIX ---
            }
        }

        Vector2 finalDir = new Vector2();
        if (!isIdling) {
            finalDir.add(wanderDirection);
        }

        if (owner instanceof model.living_beings.Animal) {
            Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce((model.living_beings.Animal) owner, world);
            if (avoidance.lengthSquared() > 0) {
                finalDir.add(avoidance);
            }
        }

        if (finalDir.lengthSquared() > 0) {
            finalDir.normalize();
            if (finalDir.x > 0) owner.setFacingRight(true);
            else if (finalDir.x < 0) owner.setFacingRight(false);
            
            owner.move(finalDir, deltaTime);
        }
    }

    public void forceStateChange() {
        this.stateTimer = 1000f; // Gán một số rất lớn để ngay lập tức kích hoạt đổi trạng thái ở frame tiếp theo
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false; // Trong Phase 1 chưa có mối đe dọa để ngắt
    }

    @Override
    public int getPriority() {
        return 0; // Ưu tiên thấp nhất
    }

    @Override
    public String getName() {
        return "Passive";
    }
}
package strategies;

import core.Vector2;
import living_beings.LivingBeing;
import world.World;
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
            // 50% đứng nghỉ, 50% đi dạo [cite: 435]
            isIdling = random.nextBoolean();
            if (isIdling) {
                stateTimer = 1.0f + random.nextFloat();
            } else {
                float dx = (random.nextFloat() * 2) - 1;
                float dy = (random.nextFloat() * 2) - 1;
                wanderDirection.set(dx, dy).normalize();
                stateTimer = 2.0f + random.nextFloat() * 2.0f;
            }
        }

        if (!isIdling) {
            owner.move(wanderDirection, deltaTime);
        }
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false; // Trong Phase 1 chưa có mối đe dọa để ngắt [cite: 435]
    }

    @Override
    public int getPriority() { return 0; } // Ưu tiên thấp nhất [cite: 41, 434]

    @Override
    public String getName() { return "Passive"; }
}
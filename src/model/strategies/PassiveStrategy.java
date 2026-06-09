package model.strategies;

import core.Vector2;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator;
import model.world.World;
import java.util.Random;

public class PassiveStrategy implements IStrategy {
    private static final float WALK_SPEED_MULTIPLIER = 0.45f;

    private Vector2 wanderDirection = new Vector2();
    private Vector2 wanderTarget = null;
    private final PathNavigator wanderNavigator = new PathNavigator();
    private float stateTimer;
    private boolean isIdling;
    private Random random = new Random();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (owner instanceof Human) {
            executeHumanPassive((Human) owner, world, deltaTime);
            return;
        }

        stateTimer -= deltaTime;
        if (stateTimer <= 0) {
            // 50% đứng nghỉ, 50% đi dạo
            isIdling = random.nextBoolean();
            if (isIdling) {
                wanderTarget = null;
                wanderNavigator.clear();
                stateTimer = 1.0f + random.nextFloat();
            } else {
                float dx = (random.nextFloat() * 2) - 1;
                float dy = (random.nextFloat() * 2) - 1;
                wanderDirection.set(dx, dy).normalize();
                float distance = 120.0f + random.nextFloat() * 180.0f;
                wanderTarget = owner.getPosition().copy().add(wanderDirection.copy().scale(distance));
                if (world != null) clampToWorld(wanderTarget, owner, world);
                stateTimer = 2.0f + random.nextFloat() * 2.0f;
            }
        }

        if (owner instanceof model.living_beings.Animal) {
            model.living_beings.Animal animal = (model.living_beings.Animal) owner;
            if (isIdling) {
                animal.setActionState("idle");
                animal.setSpeed(0);
            } else {
                animal.setActionState("walk");
                animal.setSpeed(animal.getBaseSpeed() * WALK_SPEED_MULTIPLIER);
            }
        }

        if (!isIdling && owner instanceof model.living_beings.Animal && wanderTarget != null && world != null) {
            model.living_beings.Animal animal = (model.living_beings.Animal) owner;
            boolean reached = wanderNavigator.moveTo(animal, world, wanderTarget, deltaTime, 18.0f, 2.0f);
            if (reached || wanderNavigator.isBlocked()) {
                stateTimer = 0;
                wanderTarget = null;
                wanderNavigator.clear();
            }
            return;
        }

        Vector2 finalDir = new Vector2();
        if (!isIdling) {
            finalDir.add(wanderDirection);
        }

        if (owner instanceof model.living_beings.Animal) {
            Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce((model.living_beings.Animal) owner, world, finalDir);
            if (avoidance.lengthSquared() > 0) {
                finalDir.add(avoidance);
            }
        }

        if (finalDir.lengthSquared() > 0) {
            finalDir.normalize();
            Vector2 beforeMove = owner.getPosition().copy();
            owner.move(finalDir, deltaTime);
            Vector2 actualMove = owner.getPosition().copy().subtract(beforeMove);
            if (actualMove.lengthSquared() > 0.25f) {
                if (actualMove.x > 0) owner.setFacingRight(true);
                else if (actualMove.x < 0) owner.setFacingRight(false);
            }
        }
    }

    private void executeHumanPassive(Human human, World world, float deltaTime) {
        if (human.isHidden()) {
            human.setActionState("idle");
            human.setSpeed(0);
            return;
        }

        stateTimer -= deltaTime;
        boolean outsideHome = !human.isInHomeArea();

        if (stateTimer <= 0) {
            if (outsideHome) {
                isIdling = false;
                wanderTarget = randomPointAround(human.getHomeCenter(), human.getHomeRadius() * 0.25f);
                stateTimer = 2.0f + random.nextFloat();
            } else {
                isIdling = random.nextFloat() < 0.45f;
                if (isIdling) {
                    wanderTarget = null;
                    wanderNavigator.clear();
                    stateTimer = 1.0f + random.nextFloat() * 1.5f;
                } else {
                    wanderTarget = randomPointAround(human.getHomeCenter(), human.getHomeRadius() * 0.65f);
                    stateTimer = 2.0f + random.nextFloat() * 2.0f;
                }
            }
            if (wanderTarget != null && world != null) clampToWorld(wanderTarget, human, world);
        }

        if (isIdling) {
            human.setActionState("idle");
            human.setSpeed(0);
            return;
        }

        if (wanderTarget == null) {
            stateTimer = 0;
            human.setActionState("idle");
            human.setSpeed(0);
            return;
        }

        human.setActionState("walk");
        human.setSpeed(human.getBaseSpeed() * WALK_SPEED_MULTIPLIER);
        if (world != null) {
            boolean reached = wanderNavigator.moveTo(human, world, wanderTarget, deltaTime, 18.0f, 1.5f);
            if (reached || wanderNavigator.isBlocked()) {
                stateTimer = 0;
                wanderTarget = null;
                wanderNavigator.clear();
            }
        }
    }

    private Vector2 randomPointAround(Vector2 center, float radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * Math.max(16.0f, radius);
        return new Vector2(
                (float) (center.x + Math.cos(angle) * distance),
                (float) (center.y + Math.sin(angle) * distance)
        );
    }

    public void forceStateChange() {
        this.stateTimer = 0f;
        this.wanderTarget = null;
        this.wanderNavigator.clear();
    }

    private void clampToWorld(Vector2 target, LivingBeing owner, World world) {
        float margin = owner.getSize() / 2;
        target.x = Math.max(margin, Math.min(world.getWidth() - margin, target.x));
        target.y = Math.max(margin, Math.min(world.getHeight() - margin, target.y));
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

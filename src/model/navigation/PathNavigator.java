package model.navigation;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.strategies.AvoidanceStrategy;
import model.world.World;

import java.util.ArrayList;
import java.util.List;

public class PathNavigator {
    public enum MovementContext {
        NORMAL,
        SEEKING_WATER,
        SEEKING_STRUCTURE,
        HUNTING,
        FLEEING
    }

    private final List<Vector2> path = new ArrayList<>();
    private Vector2 lastTarget = null;
    private float repathTimer = 0.0f;
    private boolean blocked = false;
    private float stuckTimer = 0.0f;
    private float losCheckTimer = 0.0f; // Line-of-sight check timer

    public boolean moveTo(Animal animal, World world, Vector2 target, float deltaTime,
                          float reachDistance, float repathInterval) {
        return moveTo(animal, world, target, deltaTime, reachDistance, repathInterval, MovementContext.NORMAL);
    }

    public boolean moveTo(Animal animal, World world, Vector2 target, float deltaTime,
                          float reachDistance, float repathInterval, MovementContext context) {
        blocked = false;
        if (animal == null || world == null || target == null || !animal.isAliveState()) return false;

        float targetDist = animal.getPosition().distanceTo(target);
        if (targetDist <= reachDistance) {
            clear();
            return true;
        }

        repathTimer -= deltaTime;
        losCheckTimer -= deltaTime;

        // Tối ưu: Chỉ kiểm tra tia thẳng mỗi 0.25s để tránh lag FPS do Raycast liên tục
        if (losCheckTimer <= 0) {
            boolean direct = TerrainNavigator.hasWalkableLine(
                    world, animal, animal.getPosition(), target, context);
            if (direct) {
                clear();
                moveAlong(animal, world, target, deltaTime, context);
                losCheckTimer = 0.25f; // Đợi 0.25s trước lần check tiếp theo
                return false;
            }
            losCheckTimer = 0.25f;
        } else if (path.isEmpty()) {
            // Nếu không có đường đi mà cũng chưa đến lúc check direct, cứ giả vờ đi thẳng xem sao
            moveAlong(animal, world, target, deltaTime, context);
            return false;
        }

        if (needsRepath(target, repathInterval)) {
            path.clear();
            List<Vector2> newPath = TerrainNavigator.findPath(world, animal, target, context);
            path.addAll(newPath);
            lastTarget = target.copy();
            repathTimer = repathInterval;
            blocked = path.isEmpty();
        }

        if (path.isEmpty()) {
            blocked = true;
            return false;
        }

        while (!path.isEmpty() && animal.getPosition().distanceTo(path.get(0)) <= Math.max(12.0f, animal.getSize() * 0.35f)) {
            path.remove(0);
        }

        if (path.isEmpty()) return false;

        // skipVisibleWaypoints(animal, world, context); // TẮT: Rất tốn kém, không cần thiết mỗi frame
        moveAlong(animal, world, path.get(0), deltaTime, context);
        return false;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void clear() {
        path.clear();
        lastTarget = null;
        repathTimer = 0.0f;
        blocked = false;
        stuckTimer = 0.0f;
    }

    private boolean needsRepath(Vector2 target, float repathInterval) {
        if (path.isEmpty() || lastTarget == null) return true;
        if (repathTimer <= 0) return true;
        return lastTarget.distanceSquared(target) > 64.0f * 64.0f;
    }

    private void skipVisibleWaypoints(Animal animal, World world, MovementContext context) {
        for (int i = path.size() - 1; i > 0; i--) {
            if (TerrainNavigator.hasWalkableLine(
                    world, animal, animal.getPosition(), path.get(i), context)) {
                for (int j = 0; j < i; j++) path.remove(0);
                return;
            }
        }
    }

    private void moveAlong(Animal animal, World world, Vector2 waypoint, float deltaTime, MovementContext context) {
        Vector2 desiredDir = waypoint.copy().subtract(animal.getPosition());
        if (desiredDir.lengthSquared() <= 0.0001f) return;
        desiredDir.normalize();

        Vector2 finalDir = desiredDir.copy();
        Vector2 avoidance = getPathAvoidance(animal, world, desiredDir, context);
        if (avoidance.lengthSquared() > 0) {
            finalDir.add(avoidance);
            if (finalDir.lengthSquared() > 0) finalDir.normalize();
        }

        Vector2 beforeMove = animal.getPosition().copy();
        animal.move(finalDir, deltaTime);

        Vector2 actualMove = animal.getPosition().copy().subtract(beforeMove);
        if (actualMove.lengthSquared() > 0.1f) {
            if (actualMove.x > 0) animal.setFacingRight(true);
            else if (actualMove.x < 0) animal.setFacingRight(false);
            stuckTimer = 0.0f;
        } else {
            // Khả năng đang bị kẹt
            stuckTimer += deltaTime;
            if (stuckTimer > 1.0f) {
                blocked = true;
                path.clear();
                stuckTimer = 0.0f;
            }
        }
    }

    private Vector2 getPathAvoidance(Animal animal, World world, Vector2 desiredDir, MovementContext context) {
        if (context == MovementContext.SEEKING_WATER) {
            return AvoidanceStrategy.getPathFollowingNonWaterAvoidanceForce(animal, world, desiredDir);
        }
        return AvoidanceStrategy.getPathFollowingAvoidanceForce(animal, world, desiredDir);
    }

    public static Vector2 findInteractionPoint(Animal actor, World world, Entity target, float interactionRange) {
        if (actor == null || world == null || target == null) return null;

        Vector2 baseDir = actor.getPosition().copy().subtract(target.getPosition());
        if (baseDir.lengthSquared() <= 0.001f) baseDir.set(1, 0);
        baseDir.normalize();

        float actorRadius = actor.getCollider() != null ? actor.getCollider().getRadius() : actor.getSize() * 0.4f;
        float targetRadius = target.getCollider() != null ? target.getCollider().getRadius() : target.getSize() * 0.4f;
        float minDistance = actorRadius + targetRadius + 0.75f;
        float desiredDistance = Math.max(minDistance, Math.min(interactionRange - 1.0f, minDistance + 12.0f));
        if (desiredDistance < minDistance) desiredDistance = minDistance;

        float[] radii = {desiredDistance, desiredDistance + 12.0f, desiredDistance + 24.0f};
        float[] angles = {0, 30, -30, 60, -60, 90, -90, 135, -135, 180};

        for (float radius : radii) {
            for (float angle : angles) {
                Vector2 dir = rotate(baseDir, angle);
                Vector2 candidate = target.getPosition().copy().add(dir.scale(radius));
                if (world.isValidPositionFor(actor, candidate)) {
                    return candidate;
                }
            }
        }

        return target.getPosition();
    }

    private static Vector2 rotate(Vector2 v, float degrees) {
        double rad = Math.toRadians(degrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new Vector2(v.x * cos - v.y * sin, v.x * sin + v.y * cos);
    }

    public java.util.List<Vector2> getPath() {
        return path;
    }

    public Vector2 getLastTarget() {
        return lastTarget;
    }
}

package model.navigation;

import core.Vector2;
import model.living_beings.Animal;
import model.strategies.AvoidanceStrategy;
import model.world.World;

import java.util.ArrayList;
import java.util.List;

public class PathNavigator {
    public enum MovementContext {
        NORMAL,
        SEEKING_WATER
    }

    private final List<Vector2> path = new ArrayList<>();
    private Vector2 lastTarget = null;
    private float repathTimer = 0.0f;
    private boolean blocked = false;

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

        boolean direct = TerrainNavigator.hasWalkableLine(world, animal, animal.getPosition(), target);
        if (direct) {
            clear();
            moveAlong(animal, world, target, deltaTime, context);
            return false;
        }

        if (needsRepath(target, repathInterval)) {
            path.clear();
            List<Vector2> newPath = TerrainNavigator.findPath(world, animal, target);
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

        skipVisibleWaypoints(animal, world);
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
    }

    private boolean needsRepath(Vector2 target, float repathInterval) {
        if (path.isEmpty() || lastTarget == null) return true;
        if (repathTimer <= 0) return true;
        return lastTarget.distanceSquared(target) > 64.0f * 64.0f;
    }

    private void skipVisibleWaypoints(Animal animal, World world) {
        for (int i = path.size() - 1; i > 0; i--) {
            if (TerrainNavigator.hasWalkableLine(world, animal, animal.getPosition(), path.get(i))) {
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
        Vector2 avoidance = (context == MovementContext.SEEKING_WATER)
                ? AvoidanceStrategy.getNonWaterAvoidanceForce(animal, world, desiredDir)
                : AvoidanceStrategy.getAvoidanceForce(animal, world, desiredDir);
        if (avoidance.lengthSquared() > 0) {
            finalDir.add(avoidance);
            if (finalDir.lengthSquared() > 0) finalDir.normalize();
        }

        Vector2 beforeMove = animal.getPosition().copy();
        animal.move(finalDir, deltaTime);

        Vector2 actualMove = animal.getPosition().copy().subtract(beforeMove);
        if (actualMove.lengthSquared() > 0.25f) {
            if (actualMove.x > 0) animal.setFacingRight(true);
            else if (actualMove.x < 0) animal.setFacingRight(false);
        }
    }
}

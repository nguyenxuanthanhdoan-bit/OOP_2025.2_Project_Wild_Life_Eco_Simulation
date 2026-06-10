package model.navigation;

import core.Vector2;
import model.living_beings.Animal;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator.MovementContext;
import model.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TerrainNavigator {
    public static final int TILE_SIZE = 32;
    private static final int DEFAULT_MAX_SEARCH_TILES = 80;
    private static final int MAX_VISITED_NODES = 9000;

    private TerrainNavigator() {}

    public static boolean isWalkable(World world, LivingBeing entity, Vector2 pos) {
        if (world == null || entity == null || pos == null) return false;
        return world.isValidPositionFor(entity, pos);
    }

    public static boolean hasWalkableLine(World world, LivingBeing entity, Vector2 from, Vector2 to) {
        return hasWalkableLine(world, entity, from, to, MovementContext.NORMAL);
    }

    public static boolean hasWalkableLine(World world, LivingBeing entity, Vector2 from, Vector2 to,
                                          MovementContext context) {
        if (world == null || entity == null || from == null || to == null) return false;

        float dist = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(dist / (TILE_SIZE * 0.5f)));
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            Vector2 sample = new Vector2(
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t
            );
            if (!isWalkable(world, entity, sample)) {
                return false;
            }
            if (traversalMultiplier(world, entity, sample, context) > 1.0f) {
                return false;
            }
        }
        return true;
    }

    public static Vector2 findNearestShorePoint(LivingBeing entity, World world, float searchRadius) {
        if (entity == null || world == null || entity.getPosition() == null) return null;

        int maxRadius = Math.max(1, (int) Math.ceil(searchRadius / TILE_SIZE));
        int startX = worldToTile(entity.getPosition().x);
        int startY = worldToTile(entity.getPosition().y);
        Vector2 best = null;
        float bestDistSq = Float.MAX_VALUE;

        for (int r = 1; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;

                    int waterX = startX + dx;
                    int waterY = startY + dy;
                    Vector2 waterCenter = tileCenter(waterX, waterY);
                    if (!world.isPositionInWater(waterCenter.x, waterCenter.y)) continue;

                    Vector2 shore = findAdjacentWalkableToWater(entity, world, waterX, waterY);
                    if (shore == null) continue;

                    float distSq = entity.getPosition().distanceSquared(shore);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        best = shore;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    public static List<Vector2> findPath(World world, LivingBeing entity, Vector2 target) {
        return findPath(world, entity, target, DEFAULT_MAX_SEARCH_TILES, MovementContext.NORMAL);
    }

    public static List<Vector2> findPath(World world, LivingBeing entity, Vector2 target,
                                         MovementContext context) {
        return findPath(world, entity, target, DEFAULT_MAX_SEARCH_TILES, context);
    }

    public static List<Vector2> findPath(World world, LivingBeing entity, Vector2 target, int maxSearchTiles) {
        return findPath(world, entity, target, maxSearchTiles, MovementContext.NORMAL);
    }

    public static List<Vector2> findPath(World world, LivingBeing entity, Vector2 target,
                                         int maxSearchTiles, MovementContext context) {
        if (world == null || entity == null || entity.getPosition() == null || target == null) {
            return Collections.emptyList();
        }

        int startX = worldToTile(entity.getPosition().x);
        int startY = worldToTile(entity.getPosition().y);
        int targetX = worldToTile(target.x);
        int targetY = worldToTile(target.y);

        TileCoord start = new TileCoord(startX, startY);
        TileCoord goal = findNearestWalkableTile(world, entity, targetX, targetY, 4);
        if (goal == null || !isTileWalkable(world, entity, start.x, start.y)) {
            return Collections.emptyList();
        }

        int padding = Math.max(8, maxSearchTiles / 3);
        int minX = Math.max(0, Math.min(start.x, goal.x) - padding);
        int maxX = Math.min(worldToTile(world.getWidth() - 1), Math.max(start.x, goal.x) + padding);
        int minY = Math.max(0, Math.min(start.y, goal.y) - padding);
        int maxY = Math.min(worldToTile(world.getHeight() - 1), Math.max(start.y, goal.y) + padding);

        PriorityQueue<PathNode> open = new PriorityQueue<>();
        Map<TileCoord, PathNode> allNodes = new HashMap<>();

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, goal));
        open.add(startNode);
        allNodes.put(start, startNode);

        int visited = 0;
        while (!open.isEmpty() && visited < MAX_VISITED_NODES) {
            PathNode current = open.poll();
            if (current.closed) continue;
            current.closed = true;
            visited++;

            if (current.coord.equals(goal)) {
                return smoothPath(reconstruct(current), world, entity, context);
            }

            for (int[] step : DIRECTIONS) {
                int nx = current.coord.x + step[0];
                int ny = current.coord.y + step[1];
                if (nx < minX || nx > maxX || ny < minY || ny > maxY) continue;
                if (!isTileWalkable(world, entity, nx, ny)) continue;
                if (step[0] != 0 && step[1] != 0) {
                    if (!isTileWalkable(world, entity, current.coord.x + step[0], current.coord.y) ||
                            !isTileWalkable(world, entity, current.coord.x, current.coord.y + step[1])) {
                        continue;
                    }
                }

                TileCoord nextCoord = new TileCoord(nx, ny);
                float stepCost = (step[0] == 0 || step[1] == 0) ? 10.0f : 14.0f;
                stepCost *= traversalMultiplier(world, entity, tileCenter(nx, ny), context);
                float nextG = current.g + stepCost;

                PathNode next = allNodes.get(nextCoord);
                if (next == null) {
                    next = new PathNode(nextCoord, current, nextG, heuristic(nextCoord, goal));
                    allNodes.put(nextCoord, next);
                    open.add(next);
                } else if (!next.closed && nextG < next.g) {
                    next.parent = current;
                    next.g = nextG;
                    next.f = nextG + next.h;
                    open.add(next);
                }
            }
        }

        return Collections.emptyList();
    }

    private static Vector2 findAdjacentWalkableToWater(LivingBeing entity, World world, int waterX, int waterY) {
        Vector2 best = null;
        float bestDistSq = Float.MAX_VALUE;

        for (int[] step : DIRECTIONS_CARDINAL) {
            int sx = waterX + step[0];
            int sy = waterY + step[1];
            Vector2 candidate = tileCenter(sx, sy);
            if (!isWalkable(world, entity, candidate)) continue;
            if (!world.isPositionInWater(candidate.x, candidate.y)) {
                float distSq = entity.getPosition().distanceSquared(candidate);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = candidate;
                }
            }
        }

        return best;
    }

    private static TileCoord findNearestWalkableTile(World world, LivingBeing entity, int tileX, int tileY, int radius) {
        if (isTileWalkable(world, entity, tileX, tileY)) return new TileCoord(tileX, tileY);

        TileCoord best = null;
        float bestDist = Float.MAX_VALUE;
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = tileX + dx;
                    int y = tileY + dy;
                    if (!isTileWalkable(world, entity, x, y)) continue;
                    float dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new TileCoord(x, y);
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static boolean isTileWalkable(World world, LivingBeing entity, int tileX, int tileY) {
        Vector2 center = tileCenter(tileX, tileY);
        return isWalkable(world, entity, center);
    }

    private static List<Vector2> reconstruct(PathNode goal) {
        List<Vector2> path = new ArrayList<>();
        PathNode current = goal;
        while (current != null) {
            path.add(tileCenter(current.coord.x, current.coord.y));
            current = current.parent;
        }
        Collections.reverse(path);
        if (!path.isEmpty()) path.remove(0);
        return path;
    }

    private static List<Vector2> smoothPath(List<Vector2> path, World world, LivingBeing entity,
                                            MovementContext context) {
        if (path.size() < 3) return path;

        List<Vector2> smoothed = new ArrayList<>();
        Vector2 anchor = entity.getPosition();
        int i = 0;
        while (i < path.size()) {
            int next = path.size() - 1;
            while (next > i) {
                if (hasWalkableLine(world, entity, anchor, path.get(next), context)) break;
                next--;
            }
            Vector2 waypoint = path.get(next);
            smoothed.add(waypoint);
            anchor = waypoint;
            i = next + 1;
        }
        return smoothed;
    }

    private static float traversalMultiplier(World world, LivingBeing entity, Vector2 pos,
                                             MovementContext context) {
        if (!(entity instanceof Animal) || world.getSettlementManager() == null
                || !world.getSettlementManager().isInsideSettlement(pos)) {
            return 1.0f;
        }

        Animal animal = (Animal) entity;
        if (animal.getProfile().getSettlementPolicy()
                != model.living_beings.AnimalProfile.SettlementPolicy.AVOID) {
            return 1.0f;
        }
        return context == MovementContext.HUNTING || context == MovementContext.FLEEING
                ? 1.0f : 8.0f;
    }

    private static float heuristic(TileCoord a, TileCoord b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        return 10.0f * (dx + dy) + (14.0f - 20.0f) * Math.min(dx, dy);
    }

    private static int worldToTile(float value) {
        return (int) Math.floor(value / TILE_SIZE);
    }

    private static Vector2 tileCenter(int tileX, int tileY) {
        return new Vector2(tileX * TILE_SIZE + TILE_SIZE / 2.0f, tileY * TILE_SIZE + TILE_SIZE / 2.0f);
    }

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private static final int[][] DIRECTIONS_CARDINAL = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private static class TileCoord {
        final int x;
        final int y;

        TileCoord(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TileCoord)) return false;
            TileCoord other = (TileCoord) obj;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return x * 73856093 ^ y * 19349663;
        }
    }

    private static class PathNode implements Comparable<PathNode> {
        final TileCoord coord;
        PathNode parent;
        float g;
        final float h;
        float f;
        boolean closed;

        PathNode(TileCoord coord, PathNode parent, float g, float h) {
            this.coord = coord;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }

        @Override
        public int compareTo(PathNode other) {
            return Float.compare(this.f, other.f);
        }
    }
}

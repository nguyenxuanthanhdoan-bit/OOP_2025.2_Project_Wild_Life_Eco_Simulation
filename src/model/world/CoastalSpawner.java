package model.world;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.map.GameMap;
import model.structures.Boat;
import model.structures.FishingHut;

import java.util.List;
import java.util.Random;

public class CoastalSpawner {

    public static void spawnCoastal(World world, GameMap gameMap, Random rand) {
        GameConfig config = GameConfig.getInstance();
        spawnFishingHuts(world, gameMap, rand, config.FISHING_HUT_COUNT);
        spawnBoats(world, gameMap, rand, config.BOAT_COUNT);
    }

    private static void spawnBoats(World world, GameMap gameMap, Random rand, int count) {
        if (gameMap == null || count <= 0) return;
        GameConfig config = GameConfig.getInstance();
        CoastalManager coastal = world.getCoastalManager();

        List<FishingHut> huts = coastal.getFishingHuts();

        int spawned = 0;
        int attempts = 0;
        int maxAttempts = count * 80;

        while (spawned < count && attempts < maxAttempts) {
            attempts++;
            
            float x, y;
            if (!huts.isEmpty()) {
                FishingHut hut = huts.get(rand.nextInt(huts.size()));
                float angle = rand.nextFloat() * (float)Math.PI * 2;
                float dist = 40f + rand.nextFloat() * 100f;
                x = hut.getPosition().x + (float)Math.cos(angle) * dist;
                y = hut.getPosition().y + (float)Math.sin(angle) * dist;
            } else {
                x = rand.nextFloat() * world.getWidth();
                y = rand.nextFloat() * world.getHeight();
            }

            if (!gameMap.isPositionInWater(x, y)) continue;

            boolean nearShore = !gameMap.isPositionInWater(x - 64, y) ||
                                !gameMap.isPositionInWater(x + 64, y) ||
                                !gameMap.isPositionInWater(x, y - 64) ||
                                !gameMap.isPositionInWater(x, y + 64);
            if (!nearShore) continue;

            Vector2 pos = new Vector2(x, y);

            if (!isFarFromBoats(coastal, pos, config.BOAT_MIN_DISTANCE)) continue;

            Boat boat = new Boat(pos);
            world.addEntity(boat);
            coastal.addBoat(boat);
            spawned++;
        }
    }

    private static void spawnFishingHuts(World world, GameMap gameMap, Random rand, int count) {
        if (gameMap == null || count <= 0) return;
        GameConfig config = GameConfig.getInstance();
        CoastalManager coastal = world.getCoastalManager();
        float shoreMaxDist = config.FISHING_HUT_SHORE_MAX_DIST;
        float minDist = config.FISHING_HUT_MIN_DISTANCE;

        int spawned = 0;
        int attempts = 0;
        int maxAttempts = count * 120;
        
        List<model.world.Settlement> settlements = world.getSettlementManager().getSettlements();

        while (spawned < count && attempts < maxAttempts) {
            attempts++;
            float x, y;
            if (settlements != null && !settlements.isEmpty()) {
                model.world.Settlement targetVillage = settlements.get(rand.nextInt(settlements.size()));
                float angle = rand.nextFloat() * (float)Math.PI * 2;
                float r = rand.nextFloat() * 800f;
                x = targetVillage.getCenter().x + (float)Math.cos(angle) * r;
                y = targetVillage.getCenter().y + (float)Math.sin(angle) * r;
            } else {
                x = rand.nextFloat() * world.getWidth();
                y = rand.nextFloat() * world.getHeight();
            }

            if (!gameMap.isValidGroundSpawnPosition(x, y, config.GROUND_SPAWN_MARGIN)) continue;

            if (!SpawnHelper.isNearWater(gameMap, x, y, shoreMaxDist)) continue;

            Vector2 pos = new Vector2(x, y);

            if (!isFarFromFishingHuts(coastal, pos, minDist)) continue;

            if (!SpawnHelper.isFarFromExistingStructures(world, pos, minDist * 0.5f)) continue;

            FishingHut hut = new FishingHut(pos);
            world.addEntity(hut);
            coastal.addFishingHut(hut);
            spawned++;
        }
    }

    private static boolean isFarFromBoats(CoastalManager coastal, Vector2 pos, float minDist) {
        for (Boat b : coastal.getBoats()) {
            if (pos.distanceTo(b.getPosition()) < minDist) return false;
        }
        return true;
    }

    private static boolean isFarFromFishingHuts(CoastalManager coastal, Vector2 pos, float minDist) {
        for (FishingHut h : coastal.getFishingHuts()) {
            if (pos.distanceTo(h.getPosition()) < minDist) return false;
        }
        return true;
    }

    public static void spawnLanterns(World world, GameMap gameMap, Random rand) {
        int minBridgeX = Integer.MAX_VALUE;
        int maxBridgeX = Integer.MIN_VALUE;
        int minBridgeY = Integer.MAX_VALUE;
        int maxBridgeY = Integer.MIN_VALUE;
        boolean bridgeFound = false;

        for (int c = 0; c < gameMap.getCols(); c++) {
            for (int r = 0; r < gameMap.getRows(); r++) {
                if (gameMap.isBridgeTile(c * 32 + 16, r * 32 + 16)) {
                    bridgeFound = true;
                    if (c * 32 < minBridgeX) minBridgeX = c * 32;
                    if (c * 32 > maxBridgeX) maxBridgeX = c * 32;
                    if (r * 32 < minBridgeY) minBridgeY = r * 32;
                    if (r * 32 > maxBridgeY) maxBridgeY = r * 32;
                }
            }
        }

        if (bridgeFound) {
            float width = maxBridgeX - minBridgeX;
            float height = maxBridgeY - minBridgeY;
            if (width > height) {
                world.addEntity(new model.structures.Lantern(new Vector2(minBridgeX - 32, (minBridgeY + maxBridgeY) / 2f + 16), "lantern"));
                world.addEntity(new model.structures.Lantern(new Vector2(maxBridgeX + 64, (minBridgeY + maxBridgeY) / 2f + 16), "lantern"));
            } else {
                world.addEntity(new model.structures.Lantern(new Vector2((minBridgeX + maxBridgeX) / 2f + 16, minBridgeY - 32), "lantern"));
                world.addEntity(new model.structures.Lantern(new Vector2((minBridgeX + maxBridgeX) / 2f + 16, maxBridgeY + 64), "lantern"));
            }
        }

        int lanternsSpawned = 0;
        int attempts = 0;
        String[] bushTypes = {"lantern_bush_1", "lantern_bush_2", "lantern_bush_3"};
        while (lanternsSpawned < 30 && attempts < 300) {
            attempts++;
            Vector2 pos = SpawnHelper.getRandomGroundPoint(world, gameMap, rand);
            if (pos != null) {
                boolean collision = false;
                for (Entity e : world.getEntities()) {
                    if (e.isSolid() && e.getPosition().distanceTo(pos) < e.getSize() / 2f + 16f) {
                        collision = true;
                        break;
                    }
                }
                if (!collision) {
                    String bushType = bushTypes[rand.nextInt(bushTypes.length)];
                    world.addEntity(new model.structures.Lantern(pos, bushType));
                    lanternsSpawned++;
                }
            }
        }
    }
}

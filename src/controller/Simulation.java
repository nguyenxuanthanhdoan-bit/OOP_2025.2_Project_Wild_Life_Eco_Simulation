package controller;

import model.world.World;
import model.map.GameMap; 
import view.systems.RenderSystem;
import view.systems.Camera;
import core.DisplayMode;
import java.awt.Graphics2D;
import core.Vector2;
import model.plants.Grass;
import model.plants.FruitTree;
import model.living_beings.Rabbit;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import java.util.Random;
import java.awt.geom.Rectangle2D;
import model.map.GameMap.MapPolygonObject;
import java.util.List;
import java.util.ArrayList;

/**
 * Trái tim điều phối toàn bộ hệ thống.
 */
public class Simulation {

    private World world;
    private RenderSystem renderSystem;
    private Camera camera;
    private DisplayMode currentDisplayMode;

    private GameMap gameMap;

    public Simulation(Camera camera, World world, RenderSystem renderSystem) {
        this.camera = camera;
        this.world = world;
        this.renderSystem = renderSystem;
        this.currentDisplayMode = DisplayMode.REALISTIC;

        initMap();
        spawnInitialEntities();
    }

    private void initMap() {
        this.gameMap = new GameMap("resources/map/map.tmx");
        this.renderSystem.setGameMap(this.gameMap);
        this.world.setGameMap(this.gameMap); 

        float realWorldWidth = gameMap.getCols() * 32f;
        float realWorldHeight = gameMap.getRows() * 32f;
        this.world.setWidth(realWorldWidth);
        this.world.setHeight(realWorldHeight);
        this.camera.setWorldBounds(realWorldWidth, realWorldHeight);
        camera.setPosition(new Vector2(realWorldWidth / 2, realWorldHeight / 2));
    }


    private void spawnInitialEntities() {
        Random rand = new Random();
        List<MapPolygonObject> polygons = gameMap.getBiomePolygons();

        List<MapPolygonObject> plainPolygons = new ArrayList<>();
        List<MapPolygonObject> forestPolygons = new ArrayList<>();
        List<MapPolygonObject> villagePolygons = new ArrayList<>();

        for (MapPolygonObject poly : polygons) {
            if ("PLAIN".equalsIgnoreCase(poly.type)) plainPolygons.add(poly);
            else if ("FOREST".equalsIgnoreCase(poly.type)) forestPolygons.add(poly);
            else if ("VILLAGE".equalsIgnoreCase(poly.type)) villagePolygons.add(poly);
        }
        
        // Backup nếu map chưa vẽ polygon
        if (plainPolygons.isEmpty()) plainPolygons = polygons;
        if (forestPolygons.isEmpty()) forestPolygons = polygons;

        // Sinh Thỏ: 80% Plain, 20% Forest (50 con)
        for (int i = 0; i < 50; i++) {
            boolean inPlain = rand.nextFloat() < 0.8f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new Rabbit(pos));
        }

        // Sinh Hươu: 60% Plain, 40% Forest (30 con, 2 đàn)
        for (int i = 0; i < 30; i++) {
            boolean inPlain = rand.nextFloat() < 0.6f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            int herdId = (i < 15) ? 1 : 2; // Đàn 1 (15 con) và Đàn 2 (15 con)
            if (pos != null) world.addEntity(new Deer(pos, herdId));
        }

        // Sinh Voi: 70% Plain, 30% Forest (10 con, 1 đàn)
        for (int i = 0; i < 10; i++) {
            boolean inPlain = rand.nextFloat() < 0.7f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new Elephant(pos, 1));
        }

        // Sinh Cỏ: 90% Plain, 10% Forest (100 bụi)
        for (int i = 0; i < 100; i++) {
            boolean inPlain = rand.nextFloat() < 0.9f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new Grass(pos));
        }

        // ===============================================
        // THUẬT TOÁN SINH CÂY (TREE CLUSTERING & BIOMES)
        // ===============================================

        // 1. Cây 2 (Gần Làng)
        for (int i = 0; i < 15; i++) {
            Vector2 pos = getPointNearVillage(plainPolygons, villagePolygons, rand, 300f);
            if (pos != null) world.addEntity(new FruitTree(pos, 2));
        }

        // 2. Cây 3, 4 (Gần Nước)
        for (int i = 0; i < 20; i++) {
            Vector2 pos = getPointNearWater(plainPolygons, rand, 200f);
            if (pos != null) {
                int type = rand.nextBoolean() ? 3 : 4;
                world.addEntity(new FruitTree(pos, type));
            }
        }

        // 3. Cây Rừng & Đồng Cỏ (1, 5, 6, 7..13)
        int[] normalTrees = {1, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        
        // Đồng cỏ: Sinh rải rác thưa thớt (20 cây)
        for (int i = 0; i < 20; i++) {
            Vector2 pos = getRandomPointInPolygons(plainPolygons, rand);
            if (pos != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                world.addEntity(new FruitTree(pos, type));
            }
        }

        // Rừng: Sinh cụm có trật tự (300 cây)
        class TreeCluster {
            Vector2 center;
            int treeType;
            TreeCluster(Vector2 c, int t) { center = c; treeType = t; }
        }
        
        List<TreeCluster> clusters = new ArrayList<>();
        // Tăng lượng tâm cụm để rừng bao phủ rộng hơn
        for (int i = 0; i < 50; i++) {
            Vector2 center = getRandomPointInPolygons(forestPolygons, rand);
            if (center != null) {
                int type = normalTrees[rand.nextInt(normalTrees.length)];
                clusters.add(new TreeCluster(center, type));
            }
        }

        if (!clusters.isEmpty()) {
            // Tăng số lượng cây rải xung quanh cụm lên rất nhiều để rừng rậm rạp
            for (int i = 0; i < 300; i++) {
                TreeCluster cluster = clusters.get(rand.nextInt(clusters.size()));
                Vector2 spawnPos = null;
                float offsetX = (rand.nextFloat() * 400f) - 200f;
                float offsetY = (rand.nextFloat() * 400f) - 200f;
                Vector2 candidate = new Vector2(cluster.center.x + offsetX, cluster.center.y + offsetY);
                
                if (gameMap != null && !gameMap.isPositionInWater(candidate.x, candidate.y)) {
                    spawnPos = candidate;
                }
                
                if (spawnPos != null) {
                    world.addEntity(new FruitTree(spawnPos, cluster.treeType));
                }
            }
        }

        // Sinh Đá: 60% Plain, 40% Forest
        for (int i = 0; i < 15; i++) {
            boolean inPlain = rand.nextFloat() < 0.6f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new model.structures.Rock(pos));
        }

        // Sinh Bụi rậm: 40% Plain, 60% Forest
        for (int i = 0; i < 30; i++) {
            boolean inPlain = rand.nextFloat() < 0.4f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new model.structures.Bush(pos));
        }

        // Sinh Hổ: 10% Plain, 90% Forest
        for (int i = 0; i < 10; i++) {
            boolean inPlain = rand.nextFloat() < 0.1f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new Tiger(pos));
        }

        // Sinh Sói: 50% Plain, 50% Forest
        for (int i = 0; i < 15; i++) {
            boolean inPlain = rand.nextFloat() < 0.5f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new Wolf(pos));
        }
    }

    private Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, Random rand) {
        if (polys == null || polys.isEmpty()) return null;
        MapPolygonObject selectedPoly = polys.get(rand.nextInt(polys.size()));
        Rectangle2D bounds = selectedPoly.polygonPath.getBounds2D();

        for (int attempt = 0; attempt < 50; attempt++) {
            float x = (float) (bounds.getX() + rand.nextDouble() * bounds.getWidth());
            float y = (float) (bounds.getY() + rand.nextDouble() * bounds.getHeight());
            if (selectedPoly.polygonPath.contains(x, y)) {
                if (gameMap != null && !gameMap.isPositionInWater(x, y)) {
                    return new Vector2(x, y);
                }
            }
        }
        return null;
    }

    private Vector2 getPointNearWater(List<MapPolygonObject> spawnPolys, Random rand, float maxDistance) {
        if (gameMap == null || spawnPolys == null || spawnPolys.isEmpty()) return null;
        for (int attempt = 0; attempt < 30; attempt++) {
            Vector2 pos = getRandomPointInPolygons(spawnPolys, rand);
            if (pos != null) {
                if (gameMap.isPositionInWater(pos.x + maxDistance, pos.y) ||
                    gameMap.isPositionInWater(pos.x - maxDistance, pos.y) ||
                    gameMap.isPositionInWater(pos.x, pos.y + maxDistance) ||
                    gameMap.isPositionInWater(pos.x, pos.y - maxDistance)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private Vector2 getPointNearVillage(List<MapPolygonObject> spawnPolys, List<MapPolygonObject> villagePolys, Random rand, float maxDistance) {
        if (spawnPolys == null || spawnPolys.isEmpty()) return null;
        if (villagePolys == null || villagePolys.isEmpty()) {
            return getRandomPointInPolygons(spawnPolys, rand); // Fallback
        }
        
        for (int attempt = 0; attempt < 30; attempt++) {
            Vector2 pos = getRandomPointInPolygons(spawnPolys, rand);
            if (pos != null) {
                for (MapPolygonObject poly : villagePolys) {
                    Rectangle2D bounds = poly.polygonPath.getBounds2D();
                    float dx = (float) Math.max(0, Math.max(bounds.getMinX() - pos.x, pos.x - bounds.getMaxX()));
                    float dy = (float) Math.max(0, Math.max(bounds.getMinY() - pos.y, pos.y - bounds.getMaxY()));
                    if (dx*dx + dy*dy < maxDistance * maxDistance) {
                        return pos;
                    }
                }
            }
        }
        return null; // Fallback có thể không sinh ra cây nếu khó tìm
    }

    public void update(float deltaTime) {
        world.update(deltaTime);
    }

    public void render(Graphics2D g2d, float deltaTime) {
        renderSystem.renderAll(world, g2d, deltaTime);
    }

    public void toggleDisplayMode() {
        if (currentDisplayMode == DisplayMode.REALISTIC) {
            currentDisplayMode = DisplayMode.MINIMAL;
        } else {
            currentDisplayMode = DisplayMode.REALISTIC;
        }
        renderSystem.setDisplayMode(currentDisplayMode);
    }
}
package controller;

import model.world.World;
import model.map.GameMap; // Nhớ import GameMap
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

    // [MỚI] Thêm biến quản lý bản đồ
    private GameMap gameMap;

    public Simulation(Camera camera, World world, RenderSystem renderSystem) {
        this.camera = camera;
        this.world = world;
        this.renderSystem = renderSystem;
        this.currentDisplayMode = DisplayMode.REALISTIC;

        // [MỚI] Khởi tạo bản đồ ngay khi game bắt đầu
        initMap();
        spawnInitialEntities();
    }

    // Hàm chuyên lo việc nạp dữ liệu bản đồ
    private void initMap() {
        this.gameMap = new GameMap("resources/map/map.tmx");
        this.renderSystem.setGameMap(this.gameMap);
        this.world.setGameMap(this.gameMap); // Gửi map sang World để check va chạm

        // 1. ĐỒNG BỘ KÍCH THƯỚC: Báo cho World biết thế giới này to bằng kích thước map
        // (Kích thước ảnh x 32 pixel mỗi ô)
        float realWorldWidth = gameMap.getCols() * 32f;
        float realWorldHeight = gameMap.getRows() * 32f;
        this.world.setWidth(realWorldWidth);
        this.world.setHeight(realWorldHeight);
        this.camera.setWorldBounds(realWorldWidth, realWorldHeight);
        // 2. DI CHUYỂN CAMERA RA GIỮA ĐẢO
        // Đặt camera ở chính giữa bản đồ thay vì góc (0,0)
        camera.setPosition(new Vector2(realWorldWidth / 2, realWorldHeight / 2));
    }

    private void spawnInitialEntities() {
        Random rand = new Random();
        List<MapPolygonObject> polygons = gameMap.getBiomePolygons();

        List<MapPolygonObject> plainPolygons = new ArrayList<>();
        List<MapPolygonObject> forestPolygons = new ArrayList<>();

        for (MapPolygonObject poly : polygons) {
            if ("PLAIN".equalsIgnoreCase(poly.type)) plainPolygons.add(poly);
            if ("FOREST".equalsIgnoreCase(poly.type)) forestPolygons.add(poly);
        }

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

        // Sinh Cây ăn quả: 30% Plain, 70% Forest (20 cây)
        for (int i = 0; i < 20; i++) {
            boolean inPlain = rand.nextFloat() < 0.3f;
            Vector2 pos = getRandomPointInPolygons(inPlain ? plainPolygons : forestPolygons, rand);
            if (pos != null) world.addEntity(new FruitTree(pos, rand.nextBoolean()));
        }
    }

    private Vector2 getRandomPointInPolygons(List<MapPolygonObject> polys, Random rand) {
        if (polys.isEmpty()) return null;
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
        return new Vector2((float)bounds.getCenterX(), (float)bounds.getCenterY());
    }

    /**
     * Cập nhật logic trong mỗi khung hình.
     */
    public void update(float deltaTime) {
        // 1. Cập nhật logic thế giới (Thỏ di chuyển, cây cỏ...)
        world.update(deltaTime);
    }

    public void render(Graphics2D g2d, float deltaTime) {
        // 2. Vẽ thế giới lên màn hình
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
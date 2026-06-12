package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Human;
import model.living_beings.HumanRole;
import model.structures.GardenBed;
import model.structures.Rock;
import model.world.World;
import view.systems.Camera;
import view.systems.render.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * =========================================================
 * KỊCH BẢN TEST: THU HOẠCH ĐỒNG LOẠT (Harvest Stress Test)
 * =========================================================
 * 
 * Mục đích: Kiểm tra hiệu năng của A* Pathfinding và hệ thống phân bổ mục tiêu.
 * - 50 Villagers tranh nhau thu hoạch 100 luống cây (Crop).
 * - Các tảng đá tạo thành vật cản trên đường đi để ép A* phải hoạt động hết công suất.
 * - Test độ sụt giảm FPS khi có quá nhiều PathNavigator tính đường đi cùng lúc.
 */
public class VillagerHarvestTest extends JPanel {

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;
    private float elapsed = 0f;

    private final Camera camera;
    private final RenderSystem renderSystem;
    private final Random random = new Random();

    private static final int MAP_W = 1200;
    private static final int MAP_H = 800;

    public VillagerHarvestTest() {
        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);
        model.world.PopulationManager.setEnabled(false);

        // 1. TẠO VƯỜN CÂY (100 GardenBeds)
        int cols = 10;
        int rows = 10;
        float startX = 200;
        float startY = 150;
        float spacing = 40;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float bx = startX + c * spacing;
                float by = startY + r * spacing;
                GardenBed bed = new GardenBed(new Vector2(bx, by));
                
                // Ép cây lớn nhanh thành MATURE
                for (int i = 0; i < 100; i++) {
                    bed.updateCrop(10.0f, world);
                }
                
                world.addEntity(bed);
                world.getCropManager().addGardenBed(bed);
            }
        }

        // 2. TẠO VẬT CẢN (Rocks)
        // Đặt vài dãy đá nằm chắn ngang giữa Làng (bên phải) và Vườn (bên trái)
        for (int i = 0; i < 8; i++) {
            world.addEntity(new Rock(new Vector2(650, 200 + i * 50)));
            world.addEntity(new Rock(new Vector2(750, 250 + i * 50)));
        }

        // 3. TẠO LÀNG NÔNG DÂN (50 Villagers)
        // Spawn phía bên phải bản đồ
        for (int i = 0; i < 50; i++) {
            float vx = 900 + random.nextFloat() * 200;
            float vy = 300 + random.nextFloat() * 400;
            Human villager = new Human(new Vector2(vx, vy), Human.Variant.MALE, HumanRole.VILLAGER, new Vector2(vx, vy), 200.0f);
            villager.setHunger(villager.getMaxHunger() * 0.5f); // Đói nhẹ để đi thu hoạch hoặc mang đồ về kho
            world.addEntity(villager);
        }

        // 4. CAMERA & RENDER SYSTEM
        camera = new Camera(MAP_W, MAP_H);
        camera.setPosition(new Vector2(MAP_W / 2f, MAP_H / 2f));

        renderSystem = new RenderSystem(camera);
        // Bật nhãn AI cho tất cả để dễ quan sát trạng thái (Harvest, Passive, Pathfinding)
        renderSystem.showStrategyLabelAll = true;

        // Vòng lặp cập nhật Game (60 FPS)
        timer = new Timer(16, e -> {
            if (!isRunning) return;
            float deltaTime = 0.016f;
            elapsed += deltaTime;

            world.update(deltaTime);
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        camera.setViewportSize(getWidth(), getHeight());

        // Nền cỏ nông trại
        g2d.setColor(new Color(140, 200, 110));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Viền map
        g2d.setColor(new Color(60, 40, 20));
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        // Render thực thể
        renderSystem.renderAll(world, g2d, 0.016f);

        // HUD thông tin
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 450, 70, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("Kịch Bản: Thu Hoạch Đồng Loạt (Harvest Stress Test)", 20, 30);
        
        long matureCrops = world.getCropManager().getGardens().stream()
                .filter(bed -> bed.getImageVariant().contains("mature")).count();
        long villagerCount = world.getEntities().stream().filter(e -> e instanceof Human && e.isAlive()).count();

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(150, 255, 150));
        g2d.drawString("Luống cây chờ thu hoạch: " + matureCrops, 20, 50);
        
        g2d.setColor(new Color(255, 200, 100));
        g2d.drawString("Nông dân: " + villagerCount, 250, 50);

        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Thời gian: %.1fs", elapsed), 20, 70);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Làng Nông Dân Thu Hoạch (Villager Harvest)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        
        VillagerHarvestTest testPanel = new VillagerHarvestTest();
        frame.add(testPanel);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                testPanel.isRunning = false;
                testPanel.timer.stop();
            }
        });

        frame.setVisible(true);
    }
}

package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Rabbit;
import model.living_beings.Wolf;
import model.living_beings.Tiger;
import model.living_beings.Human;
import model.living_beings.HumanRole;
import model.structures.Rock;
import model.structures.Bush;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * =========================================================
 * KỊCH BẢN TEST: SĂN TRONG BẦY (Multi-Prey Hunt)
 * =========================================================
 * 
 * Mục đích: Kiểm tra hành vi của động vật ăn thịt (Sói, Hổ) khi đứng trước SỐ LƯỢNG LỚN con mồi.
 * - Thú săn mồi có bị "nhiễu loạn" (chạy qua chạy lại giữa 2 con mồi) không?
 * - Khi đuổi theo 1 con, các con mồi khác xung quanh phản ứng thế nào (ScaredStrategy bầy đàn)?
 * - Động vật ăn thịt sẽ chọn mục tiêu dựa trên tiêu chí nào (gần nhất / dễ bắt nhất)?
 */
public class MultiPreyHuntTest extends JPanel {

    // Tối ưu hóa: Khởi tạo Font 1 lần duy nhất
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font TEXT_FONT = new Font("SansSerif", Font.PLAIN, 12);

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;
    private float elapsed = 0f;

    private final Camera camera;
    private final RenderSystem renderSystem;
    private final Random random = new Random();

    private static final int MAP_W = 1000;
    private static final int MAP_H = 750;

    public MultiPreyHuntTest() {
        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);
        model.world.PopulationManager.setEnabled(false);

        // 1. TẠO BẦY CON MỒI (Prey)
        // Rải 15 con Thỏ và 10 con Hươu khắp tâm bản đồ
        for (int i = 0; i < 15; i++) {
            float rx = MAP_W/2f + (random.nextFloat() * 400 - 200);
            float ry = MAP_H/2f + (random.nextFloat() * 400 - 200);
            Rabbit rabbit = new Rabbit(new Vector2(rx, ry));
            rabbit.setHunger(rabbit.getMaxHunger()); // Cho mồi no để chúng không bị phân tâm tìm đồ ăn
            world.addEntity(rabbit);
        }

        for (int i = 0; i < 10; i++) {
            float rx = MAP_W/2f + (random.nextFloat() * 500 - 250);
            float ry = MAP_H/2f + (random.nextFloat() * 500 - 250);
            Deer deer = new Deer(new Vector2(rx, ry));
            deer.setHunger(deer.getMaxHunger());
            world.addEntity(deer);
        }

        // 2. TẠO THÚ SĂN MỒI (Predators)
        // 1 con Hổ (Cực đói) thả ở góc trái
        Tiger tiger = new Tiger(new Vector2(100, MAP_H / 2f));
        tiger.setHunger(1.0); 
        world.addEntity(tiger);

        // 2 con Sói (Cực đói) thả ở góc phải
        Wolf wolf1 = new Wolf(new Vector2(MAP_W - 100, MAP_H / 2f - 50));
        wolf1.setHunger(1.0);
        world.addEntity(wolf1);

        Wolf wolf2 = new Wolf(new Vector2(MAP_W - 100, MAP_H / 2f + 50));
        wolf2.setHunger(1.0);
        world.addEntity(wolf2);

        // 3. TẠO CHƯỚNG NGẠI VẬT NHẸ (Để test khả năng lùa mồi vào góc)
        world.addEntity(new Rock(new Vector2(200, 200)));
        world.addEntity(new Rock(new Vector2(800, 600)));
        world.addEntity(new Rock(new Vector2(500, 150)));
        world.addEntity(new Rock(new Vector2(500, 600)));

        // 3.5. TẠO CON NGƯỜI (Humans)
        // 5 Dân làng đi dạo
        for (int i = 0; i < 5; i++) {
            float hx = MAP_W/2f + (random.nextFloat() * 400 - 200);
            float hy = MAP_H/2f + (random.nextFloat() * 400 - 200);
            Human villager = new Human(new Vector2(hx, hy), 
                random.nextBoolean() ? Human.Variant.MALE : Human.Variant.FEMALE,
                HumanRole.VILLAGER, new Vector2(MAP_W/2f, MAP_H/2f), 800f);
            villager.setHunger(villager.getMaxHunger());
            world.addEntity(villager);
        }

        // 4. THÊM BỤI CỎ ĐỂ ĐỘNG VẬT ẨN NẤP
        world.addEntity(new Bush(new Vector2(300, 400)));
        world.addEntity(new Bush(new Vector2(700, 300)));
        world.addEntity(new Bush(new Vector2(150, 600)));
        world.addEntity(new Bush(new Vector2(600, 700)));

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);
        renderSystem.showStrategyLabelAll = true;

        timer = new Timer(16, e -> {
            if (isRunning) {
                elapsed += 0.016f;
                world.update(0.016f);
                repaint();
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        camera.setViewportSize(getWidth(), getHeight());

        // Nền cỏ thảo nguyên
        g2d.setColor(new Color(130, 190, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Viền map
        g2d.setColor(new Color(60, 40, 20));
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        // Render thực thể
        renderSystem.renderAll(world, g2d, 0.016f);

        // HUD thông tin
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 400, 70, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(TITLE_FONT);
        g2d.drawString("Kịch Bản: Săn Mồi Trong Bầy Lớn", 20, 30);
        
        long rabbitCount = 0;
        long deerCount = 0;
        long predatorCount = 0;
        long villagerCount = 0;
        
        for (Entity e : world.getEntities()) {
            if (!e.isAlive()) continue;
            if (e instanceof Rabbit) rabbitCount++;
            else if (e instanceof Deer) deerCount++;
            else if (e instanceof Tiger || e instanceof Wolf) predatorCount++;
            else if (e instanceof Human) villagerCount++;
        }

        g2d.setFont(TEXT_FONT);
        g2d.setColor(new Color(255, 100, 100));
        g2d.drawString("Predators: " + predatorCount, 20, 50);
        
        g2d.setColor(new Color(200, 200, 255));
        g2d.drawString("Thỏ còn sống: " + rabbitCount + "/15", 220, 50);
        
        g2d.setColor(new Color(255, 230, 150));
        g2d.drawString("Hươu còn sống: " + deerCount + "/10", 350, 50);
        
        g2d.setColor(new Color(150, 255, 150));
        g2d.drawString("Dân làng: " + villagerCount + "/5", 20, 70);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString("Thời gian: " + (Math.round(elapsed * 10) / 10.0) + "s", 120, 70);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Đàn Thú Bỏ Chạy & Kẻ Săn Mồi (Multi-Prey)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        MultiPreyHuntTest panel = new MultiPreyHuntTest();
        panel.setPreferredSize(new Dimension(MAP_W, MAP_H));

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.isRunning = false;
                panel.timer.stop();
                System.out.println("Window closed. Test terminated.");
                System.exit(0);
            }
        });
    }
}

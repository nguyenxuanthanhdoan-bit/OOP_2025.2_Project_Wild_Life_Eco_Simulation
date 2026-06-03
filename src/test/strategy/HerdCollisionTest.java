package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.plants.Grass;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * Kịch bản "Hành vi Đám đông" (Flocking / Herd Behavior).
 *
 * Mục đích: Kiểm tra sự va chạm nội bộ (Local Collision Avoidance).
 * - 15 con Hươu đói ở các vị trí ngẫu nhiên phía bên trái.
 * - Chỉ có DUY NHẤT 1 bãi cỏ ở bên phải.
 * - Khi cả 15 con cùng ùa về một hướng, liệu chúng có tự động giãn cách,
 *   né nhau để không bị chồng chéo hình ảnh lên nhau hay không?
 * - Nếu hình ảnh chồng chéo → Separation logic cần được củng cố.
 */
public class HerdCollisionTest extends JPanel {

    private static final int MAP_W = 700;
    private static final int MAP_H = 600;
    private static final int DEER_COUNT = 15;

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;

    private final Camera camera;
    private final RenderSystem renderSystem;

    public HerdCollisionTest() {
        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);

        model.world.PopulationManager.setEnabled(false);

        Random rand = new Random(42); // Seed cố định để lặp lại được

        // 1. Rải 15 con Hươu ở khu vực bên trái (x: 50-250, y: 50-550)
        //    Tất cả đều cực kỳ đói để bắt buộc chạy về phía cỏ
        for (int i = 0; i < DEER_COUNT; i++) {
            float x = 50 + rand.nextFloat() * 200;
            float y = 50 + rand.nextFloat() * 500;
            Deer d = new Deer(new Vector2(x, y));
            d.setHunger(3.0); // Cực kỳ đói
            world.addEntity(d);
        }

        // 2. Chỉ 1 bãi cỏ duy nhất ở bên phải
        world.addEntity(new Grass(new Vector2(MAP_W - 80, MAP_H / 2.0f)));

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        timer = new Timer(16, e -> {
            if (isRunning) {
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

        // Nền cỏ xanh
        g2d.setColor(new Color(120, 185, 110));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Viền khép kín
        g2d.setColor(new Color(80, 55, 30));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        // Render tất cả entity
        renderSystem.renderAll(world, g2d, 0.016f);

        // Đếm số cặp chồng chéo (overlap) thực sự để hiển thị metric
        java.util.List<Entity> entities = world.getEntities();
        int overlapCount = 0;
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                Entity a = entities.get(i);
                Entity b = entities.get(j);
                if (!(a instanceof Deer) || !(b instanceof Deer)) continue;
                if (!a.isAlive() || !b.isAlive()) continue;
                float dist = a.getPosition().distanceTo(b.getPosition());
                float minDist = a.getSize() / 2 + b.getSize() / 2;
                if (dist < minDist) overlapCount++;
            }
        }

        // Header info
        long deerAlive = entities.stream().filter(e -> e instanceof Deer && e.isAlive()).count();
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2d.drawString("Hươu: " + deerAlive + "/" + DEER_COUNT + "  |  1 bãi cỏ duy nhất", 10, 22);

        // Số cặp chồng chéo — màu đỏ nếu > 0, màu xanh nếu = 0
        if (overlapCount > 0) {
            g2d.setColor(new Color(255, 80, 80));
            g2d.drawString("⚠ Cặp chồng chéo: " + overlapCount + " (Separation cần cải thiện!)", 10, 42);
        } else {
            g2d.setColor(new Color(80, 255, 120));
            g2d.drawString("✓ Không có chồng chéo (Separation hoạt động tốt!)", 10, 42);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Herd Collision — 15 Hươu vs 1 bãi cỏ");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        HerdCollisionTest panel = new HerdCollisionTest();
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

package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.animal.Animal;
import model.living_beings.Rabbit;
import model.living_beings.Wolf;
import model.structures.Rock;
import model.world.World;
import model.strategies.HunterStrategy;
import view.systems.Camera;
import view.systems.render.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Test kịch bản Rượt đuổi: Đường đua vòng tròn (Sói đuổi Thỏ).
 * - Xây dựng một đường đua vòng tròn bằng các tảng đá (vòng trong và vòng ngoài).
 * - Thỏ và Sói bị nhốt trong đường đua này.
 * - Sói cực kỳ đói sẽ tìm cách rượt Thỏ.
 * - Thỏ sẽ phải chạy trốn dọc theo đường đua.
 */
public class PredatorPreyTest extends JPanel {

    private final World world;
    private final Wolf predator;
    private final Rabbit prey;
    private final Timer timer;
    private boolean isRunning = true;
    private float elapsed = 0f;

    private final Camera camera;
    private final RenderSystem renderSystem;

    private static final int MAP_W = 700;
    private static final int MAP_H = 700;

    public PredatorPreyTest() {
        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);

        // Tắt auto-spawn
        model.world.PopulationManager.setEnabled(false);

        // 1. Xây dựng đường đua vòng tròn bằng Đá (Rock)
        float centerX = MAP_W / 2.0f;
        float centerY = MAP_H / 2.0f;
        float innerRadius = 100.0f;
        float outerRadius = 250.0f;
        int innerCount = 16; // Số lượng đá vòng trong
        int outerCount = 36; // Số lượng đá vòng ngoài

        // Vòng trong
        for (int i = 0; i < innerCount; i++) {
            double angle = 2 * Math.PI * i / innerCount;
            float x = centerX + (float) (Math.cos(angle) * innerRadius);
            float y = centerY + (float) (Math.sin(angle) * innerRadius);
            world.addEntity(new Rock(new Vector2(x, y)));
        }

        // Vòng ngoài
        for (int i = 0; i < outerCount; i++) {
            double angle = 2 * Math.PI * i / outerCount;
            float x = centerX + (float) (Math.cos(angle) * outerRadius);
            float y = centerY + (float) (Math.sin(angle) * outerRadius);
            world.addEntity(new Rock(new Vector2(x, y)));
        }

        // Bịt các góc bản đồ để chúng không thoát ra ngoài đường đua (tùy chọn)

        // 2. Thả Thỏ vào đường đua (góc 0 độ, bên phải)
        float trackRadius = (innerRadius + outerRadius) / 2.0f;
        prey = new Rabbit(new Vector2(centerX + trackRadius, centerY));
        prey.setHunger(prey.getMaxHunger()); // Thỏ no, chỉ tập trung chạy trốn
        world.addEntity(prey);

        // 3. Thả Sói vào đường đua, đuổi theo phía sau Thỏ (góc 180 độ, bên trái)
        predator = new Wolf(new Vector2(centerX - trackRadius, centerY));
        predator.setHunger(1.0); // Rất đói, ép buộc đi săn ngay
        predator.setStrategy(new HunterStrategy());
        world.addEntity(predator);

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

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

        // Nền đường đất (màu cát/đất)
        g2d.setColor(new Color(200, 160, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Cỏ bên ngoài và bên trong vòng tròn
        g2d.setColor(new Color(120, 185, 110));
        // Lấp cỏ khu vực vòng trong
        g2d.fillOval((int)(MAP_W/2 - 100), (int)(MAP_H/2 - 100), 200, 200);

        // Render thực thể
        renderSystem.renderAll(world, g2d, 0.016f);

        // Thanh máu
        g2d.setStroke(new BasicStroke(1));
        for (Entity e : world.getEntities()) {
            if (!(e instanceof Animal) || !e.isAlive()) continue;
            Animal a = (Animal) e;
            float hpRatio = (float) (a.getHealth() / a.getMaxHealth());
            int barW = 30, barH = 4;
            int bx = (int) a.getPosition().x - barW / 2;
            int by = (int) a.getPosition().y - (int) a.getSize() / 2 - 10;

            g2d.setColor(new Color(180, 30, 30));
            g2d.fillRect(bx, by, barW, barH);
            g2d.setColor(new Color(60, 200, 60));
            g2d.fillRect(bx, by, (int) (barW * hpRatio), barH);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(bx, by, barW, barH);
        }

        // HUD thông tin
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 300, 70, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("Đường Đua Vòng Tròn: Sói đuổi Thỏ", 20, 30);
        
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(255, 100, 100));
        g2d.drawString("Sói: " + (predator.isAlive() ? "Đang săn" : "Chết"), 20, 50);
        g2d.setColor(new Color(150, 255, 150));
        g2d.drawString("Thỏ: " + (prey.isAlive() ? "Đang chạy" : "Chết"), 150, 50);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Thời gian: %.1fs", elapsed), 20, 70);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Vòng Đua Sinh Tử (Predator vs Prey)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        PredatorPreyTest panel = new PredatorPreyTest();
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

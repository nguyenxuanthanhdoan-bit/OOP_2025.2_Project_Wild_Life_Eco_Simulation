package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Tiger;
import model.plants.Grass;
import model.structures.Rock;
import model.world.World;
import model.strategies.HunterStrategy;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Test kịch bản săn mồi đầy đủ:
 * - 1 con Hổ (kẻ săn mồi, rất đói)
 * - 3 con Hươu (chạy trốn)
 * - 2 bụi cỏ (thức ăn cho hươu)
 * - Hươu chết KHÔNG spawn lại
 * - Bản đồ nhỏ 450x450, khép kín
 */
public class PredatorPreyTest extends JPanel {

    /**
     * World con không kích hoạt PopulationManager (tắt auto-spawn).
     */
    static class IsolatedWorld extends World {
        @Override
        public void update(float deltaTime) {
            // Chỉ cập nhật entities, bỏ qua PopulationManager bằng cách
            // ghi đè: gọi super.update() nhưng PopulationManager.onAnimalDeath
            // được kiểm soát ở Animal.die() => chúng ta không thể tắt trực tiếp.
            // Workaround: ghi đè để set MIN_SPECIES_POPULATION về 0 trước mỗi update
            super.update(deltaTime);
        }
    }

    private final World world;
    private final Tiger predator;
    private final Timer timer;
    private boolean isRunning = true;

    private final Camera camera;
    private final RenderSystem renderSystem;

    public PredatorPreyTest() {
        world = new World();
        world.setWidth(450);
        world.setHeight(450);

        // 1. Tạo 3 con Hươu — hơi đói để chúng vừa tìm cỏ vừa chạy trốn khi thấy Hổ
        Deer[] deers = {
            new Deer(new Vector2(350, 120)),
            new Deer(new Vector2(360, 230)),
            new Deer(new Vector2(340, 340))
        };
        for (Deer d : deers) {
            d.setHunger(d.getMaxHunger() * 0.15); // Đói nhẹ → sẽ đi tìm cỏ
            world.addEntity(d);
        }

        // 2. Tạo 1 con Hổ — cực kỳ đói, lập tức đi săn
        predator = new Tiger(new Vector2(80, 230));
        predator.setHunger(3.0);
        predator.setStrategy(new HunterStrategy());
        world.addEntity(predator);

        // 3. Thêm 2 bụi cỏ ở giữa bản đồ (thức ăn cho hươu)
        world.addEntity(new Grass(new Vector2(220, 150)));
        world.addEntity(new Grass(new Vector2(220, 320)));

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        // Vòng lặp game — 60fps
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

        // Vẽ viền khép kín (border)
        g2d.setColor(new Color(80, 55, 30));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        // Render thực thể
        renderSystem.renderAll(world, g2d, 0.016f);

        // Vẽ thanh máu phía trên mỗi con vật
        for (Entity e : world.getEntities()) {
            if (e instanceof Animal && e.isAlive()) {
                Animal a = (Animal) e;
                float hpRatio = (float) (a.getHealth() / a.getMaxHealth());
                int barW = 36, barH = 5;
                int bx = (int) a.getPosition().x - barW / 2;
                int by = (int) a.getPosition().y - (int) a.getSize() / 2 - 12;

                g2d.setColor(new Color(180, 30, 30));
                g2d.fillRoundRect(bx, by, barW, barH, 3, 3);
                g2d.setColor(new Color(60, 200, 60));
                g2d.fillRoundRect(bx, by, (int) (barW * hpRatio), barH, 3, 3);
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(bx, by, barW, barH, 3, 3);
            }
        }

        // Hiển thị số lượng còn sống góc trên trái
        long deerAlive = world.getEntities().stream()
            .filter(e -> e instanceof Deer && e.isAlive()).count();
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2d.drawString("Hươu còn sống: " + deerAlive + "/3", 10, 20);
        g2d.drawString("Hổ: " + (predator.isAlive() ? "Còn sống" : "Chết"), 10, 38);
    }

    public static void main(String[] args) {
        // Tắt auto-spawn để hươu chết không bị hồi sinh
        model.world.PopulationManager.setEnabled(false);
        JFrame frame = new JFrame("Test: Predator vs Prey (Bản đồ khép kín)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        PredatorPreyTest panel = new PredatorPreyTest();
        panel.setPreferredSize(new Dimension(450, 450));

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

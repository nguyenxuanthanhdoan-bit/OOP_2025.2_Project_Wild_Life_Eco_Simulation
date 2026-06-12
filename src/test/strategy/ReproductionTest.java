package test.strategy;

import core.Vector2;
import model.living_beings.Rabbit;
import model.world.World;
import view.systems.Camera;
import view.systems.render.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Mục đích: Kiểm tra khả năng sinh sản của 2 con thỏ khi thỏa mãn đầy đủ điều kiện.
 */
public class ReproductionTest extends JPanel {

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;
    
    private final Camera camera;
    private final RenderSystem renderSystem;

    public ReproductionTest() {
        world = new World();
        world.setWidth(800);
        world.setHeight(600);

        // Tạo 2 con thỏ ở gần nhau
        Rabbit rabbit1 = new Rabbit(new Vector2(350, 300));
        Rabbit rabbit2 = new Rabbit(new Vector2(450, 300));

        // Setup các điều kiện hoàn hảo để sinh sản:
        // 1. Phải là người lớn (age >= maxAge * 0.2). Với maxAge = 900 -> trưởng thành khi >= 180
        rabbit1.setAge(200);
        rabbit1.setAdult(true);
        rabbit2.setAge(200);
        rabbit2.setAdult(true);

        // 2. Phải no và không khát (hunger >= maxHunger * 0.7, thirst >= maxThirst * 0.7)
        rabbit1.setHunger(100);
        rabbit1.setThirst(100);
        rabbit2.setHunger(100);
        rabbit2.setThirst(100);

        world.addEntity(rabbit1);
        world.addEntity(rabbit2);

        // Setup View
        camera = new Camera(800, 600);
        camera.setPosition(new Vector2(400, 300)); // Focus vào giữa màn hình
        renderSystem = new RenderSystem(camera);

        this.setPreferredSize(new Dimension(800, 600));

        // Game Loop (60 FPS)
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
        // Tùy chọn tăng chất lượng render
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        renderSystem.renderAll(world, g2d, 0.016f);

        // Hiển thị HUD cơ bản
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Reproduction Test (Thỏ sinh sản)", 20, 30);
        
        // Đếm tổng số lượng thú
        long animalCount = world.getEntities().stream()
                .filter(e -> e instanceof model.living_beings.animal.Animal && e.isAlive())
                .count();
        g2d.drawString("Tổng số thú hiện tại: " + animalCount, 20, 50);

        if (animalCount > 2) {
            g2d.setColor(Color.GREEN);
            g2d.drawString("-> ĐÃ SINH SẢN THÀNH CÔNG THỎ CON!", 20, 70);
        }
    }

    public static void main(String[] args) {
        // Init không cần thiết ở đây

        JFrame frame = new JFrame("Reproduction Test");
        ReproductionTest testPanel = new ReproductionTest();
        frame.add(testPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                testPanel.isRunning = false;
                testPanel.timer.stop();
                frame.dispose();
                System.exit(0);
            }
        });
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

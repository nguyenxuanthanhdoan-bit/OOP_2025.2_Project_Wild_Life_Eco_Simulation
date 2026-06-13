package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Rabbit;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Mushroom;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * Mục đích: Kiểm tra khả năng sinh sản của 2 con thỏ khi thỏa mãn đầy đủ điều kiện.
 */
public class ReproductionTest extends JPanel {

    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 12);

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;
    
    private final Camera camera;
    private final RenderSystem renderSystem;

    public ReproductionTest() {
        world = new World() {
            @Override
            public boolean isPositionInWater(float x, float y) {
                // Tạo một hồ nước ảo hình tròn ở tọa độ (600, 300), bán kính 80
                float dx = x - 600;
                float dy = y - 300;
                return (dx * dx + dy * dy) <= 6400; // 80 * 80
            }
        };
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

        // Thêm thức ăn (Cỏ, Nấm, Quả) rải rác xung quanh để thỏ hồi sức sau khi đẻ
        Random rand = new Random();
        for (int i = 0; i < 15; i++) {
            float x = 200 + rand.nextFloat() * 400;
            float y = 150 + rand.nextFloat() * 300;
            world.addEntity(new Grass(new Vector2(x, y)));
        }
        for (int i = 0; i < 8; i++) {
            float x = 200 + rand.nextFloat() * 400;
            float y = 150 + rand.nextFloat() * 300;
            world.addEntity(new Mushroom(new Vector2(x, y)));
        }
        for (int i = 0; i < 8; i++) {
            float x = 200 + rand.nextFloat() * 400;
            float y = 150 + rand.nextFloat() * 300;
            world.addEntity(new Fruit(new Vector2(x, y)));
        }

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
        
        // Vẽ nền hồ nước
        g2d.setColor(new Color(64, 164, 223, 180));
        g2d.fillOval(520, 220, 160, 160); // Vẽ hình tròn đường kính 160 tại tọa độ tâm (600, 300)
        
        renderSystem.renderAll(world, g2d, 0.016f);

        // Hiển thị HUD cơ bản
        g2d.setColor(Color.WHITE);
        g2d.setFont(TITLE_FONT);
        g2d.drawString("Reproduction Test (Thỏ sinh sản)", 20, 30);
        
        // Đếm tổng số lượng thú (Tối ưu: dùng vòng lặp for thay vì Stream)
        long animalCount = 0;
        for (Entity e : world.getEntities()) {
            if (e instanceof Animal && e.isAlive()) {
                animalCount++;
            }
        }
        
        g2d.setFont(TEXT_FONT);
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

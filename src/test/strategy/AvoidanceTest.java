package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.plants.Fruit;
import model.structures.Bush;
import model.structures.Rock;
import model.world.World;
import model.strategies.ForageStrategy;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * Mục đích: Kiểm tra khả năng né vật cản của động vật khi đi tìm thức ăn.
 * Cập nhật: Sử dụng RenderSystem để vẽ hình ảnh thực tế, chướng ngại vật rải rác ngẫu nhiên.
 */
public class AvoidanceTest extends JPanel {

    private final World world;
    private final Deer testDeer;
    private final Fruit targetFruit;
    private final Timer timer;
    private boolean isRunning = true;
    
    private final Camera camera;
    private final RenderSystem renderSystem;

    public AvoidanceTest() {
        world = new World();
        world.setWidth(800);
        world.setHeight(600);

        // 1. Tạo con hươu đói ở bên trái
        testDeer = new Deer(new Vector2(100, 300));
        testDeer.setHunger(10.0); // Ép cho Hươu phải đi kiếm ăn
        testDeer.setThirst(150.0);
        testDeer.setStrategy(new ForageStrategy());
        world.addEntity(testDeer);

        // 2. Tạo đồ ăn ở bên phải
        targetFruit = new Fruit(new Vector2(700, 300));
        world.addEntity(targetFruit);

        // 3. Khởi tạo ngẫu nhiên nhiều cây bụi và đá chắn ở giữa, tạo ra các khe hở
        Random rand = new Random(); 
        for (int i = 0; i < 20; i++) {
            float x = 250 + rand.nextFloat() * 350; // X từ 250 đến 600
            float y = 50 + rand.nextFloat() * 500; // Y từ 50 đến 550
            
            // Random tạo ra khe hở lớn ở giữa để có nhiều đường đi
            if (x > 380 && x < 420 && y > 250 && y < 350) continue;
            
            if (rand.nextBoolean()) {
                world.addEntity(new Bush(new Vector2(x, y)));
            } else {
                world.addEntity(new Rock(new Vector2(x, y)));
            }
        }
        
        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        // Vòng lặp game
        timer = new Timer(16, e -> {
            if (isRunning) {
                world.update(0.016f); // Update logic
                repaint(); // Cập nhật màn hình
                
                // Dừng nếu hươu đã ăn xong 
                if (testDeer.getHunger() > 50.0 || !targetFruit.isAlive()) {
                    System.out.println("TEST PASSED: Deer successfully navigated and reached the food!");
                    isRunning = false;
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Fix lỗi màn hình xanh: Bắt buộc set Viewport cho camera vì RenderSystem bỏ qua set Viewport nếu GameMap = null
        camera.setViewportSize(getWidth(), getHeight());
        
        // Vẽ nền cỏ xanh để render hình ảnh lên trên
        g2d.setColor(new Color(153, 204, 153)); 
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Sử dụng hệ thống render thật của game để vẽ hình ảnh (sprites)
        renderSystem.renderAll(world, g2d, 0.016f);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test 1: Obstacle Avoidance (Visuals)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        AvoidanceTest panel = new AvoidanceTest();
        panel.setPreferredSize(new Dimension(800, 600));
        
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

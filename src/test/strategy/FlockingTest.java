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

/**
 * Mục đích: Kiểm tra hành vi Đám đông (Flocking) và Tránh va chạm giữa các con vật.
 * - Thả 3 con Hươu đang cực kỳ đói.
 * - Chỉ cung cấp 2 bụi cỏ làm thức ăn.
 * - Kiểm tra xem khi cùng đổ xô về một bụi cỏ, chúng có tự động giãn cách để không bị đè hình lên nhau hay không.
 */
public class FlockingTest extends JPanel {

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;
    
    private final Camera camera;
    private final RenderSystem renderSystem;

    public FlockingTest() {
        world = new World();
        world.setWidth(600);
        world.setHeight(600);

        // 1. Tạo 3 con Hươu đang rất đói ở các vị trí khác nhau
        for (int i = 0; i < 3; i++) {
            Deer deer = new Deer(new Vector2(100, 100 + i * 150));
            deer.setHunger(5.0); // Ép buộc đi tìm thức ăn
            world.addEntity(deer);
        }

        // 2. Chỉ cung cấp 2 bụi cỏ (thức ăn) ở phía đối diện
        world.addEntity(new Grass(new Vector2(500, 200)));
        world.addEntity(new Grass(new Vector2(500, 400)));
        
        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        // Vòng lặp game
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
        
        camera.setViewportSize(getWidth(), getHeight());
        
        // Vẽ nền cỏ xanh 
        g2d.setColor(new Color(153, 204, 153)); 
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Sử dụng hệ thống render thật của game 
        renderSystem.renderAll(world, g2d, 0.016f);
        
        // Vẽ thêm thanh máu để dễ theo dõi
        for (Entity e : world.getEntities()) {
            if (e instanceof Animal && e.isAlive()) {
                Animal a = (Animal) e;
                float hpRatio = (float) (a.getHealth() / a.getMaxHealth());
                int barWidth = 40;
                int barHeight = 6;
                int x = (int) a.getPosition().x - barWidth / 2;
                int y = (int) a.getPosition().y - (int) a.getSize() / 2 - 15;
                
                g2d.setColor(Color.RED);
                g2d.fillRect(x, y, barWidth, barHeight);
                g2d.setColor(Color.GREEN);
                g2d.fillRect(x, y, (int) (barWidth * hpRatio), barHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, barWidth, barHeight);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test 3: Flocking & Collision Avoidance");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        FlockingTest panel = new FlockingTest();
        panel.setPreferredSize(new Dimension(600, 600));
        
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

package test.strategy;

import core.Vector2;
import model.living_beings.Rabbit;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Mục đích: Kiểm tra khả năng tìm nguồn nước và uống nước của động vật.
 */
public class ThirstTest extends JPanel {

    private final World world;
    private final Timer timer;
    private boolean isRunning = true;
    
    private final Camera camera;
    private final RenderSystem renderSystem;

    public ThirstTest() {
        // Ghi đè isPositionInWater để giả lập một hồ nước ở giữa bản đồ
        world = new World() {
            @Override
            public boolean isPositionInWater(float x, float y) {
                // Tâm bản đồ 400x300, bán kính hồ nước = 80
                float dx = x - 400;
                float dy = y - 300;
                return (dx * dx + dy * dy) <= 6400; 
            }
        };
        
        float realWorldWidth = 800f;
        float realWorldHeight = 600f;
        world.setWidth(realWorldWidth);
        world.setHeight(realWorldHeight);

        // Tạo 1 con thỏ ở góc trái trên, xa hồ nước
        Rabbit rabbit = new Rabbit(new Vector2(100, 100));

        // Thiết lập trạng thái: Rất khát, nhưng không đói
        rabbit.setHunger(rabbit.getMaxHunger()); 
        rabbit.setThirst(10); // Rất khát
        
        world.addEntity(rabbit);

        // Setup View
        camera = new Camera(800, 600);
        camera.setWorldBounds(realWorldWidth, realWorldHeight);
        camera.setPosition(new Vector2(400, 300)); // Focus giữa bản đồ
        
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Cập nhật viewport
        camera.setViewportSize(getWidth(), getHeight());
        
        // Nền cỏ xanh lá
        g2d.setColor(new Color(130, 190, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Vẽ hồ nước giả lập ở tâm bản đồ
        g2d.setColor(new Color(60, 140, 240)); // Màu xanh nước biển
        Vector2 screenPos = camera.worldToScreen(new Vector2(400, 300));
        float zoom = camera.getZoomLevel();
        int r = (int)(80 * zoom); // Bán kính 80
        g2d.fillOval((int)screenPos.x - r, (int)screenPos.y - r, r * 2, r * 2);

        // Render world (Thỏ)
        renderSystem.renderAll(world, g2d, 0.016f);

        // Hiển thị HUD cơ bản
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Thirst Test (Kiểm tra tìm nguồn nước)", 20, 30);
        g2d.drawString("Quan sát thanh màu xanh dương (Khát). Khi tìm thấy nước, thỏ sẽ đứng uống.", 20, 50);
    }

    public static void main(String[] args) {
        // Khởi tạo GameConfig
        // core.GameConfig.getInstance().init();

        JFrame frame = new JFrame("Thirst Test");
        ThirstTest testPanel = new ThirstTest();
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

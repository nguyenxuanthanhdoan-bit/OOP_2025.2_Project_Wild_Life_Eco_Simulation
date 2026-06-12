package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Human;
import model.living_beings.HumanRole;
import model.structures.Boat;
import model.strategies.BoardBoatStrategy;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Mục đích: Kiểm tra thao tác ra biển thả lưới của ngư dân (Fisherman).
 * Ngư dân sẽ lên thuyền, thuyền chạy ra vùng nước, thả lưới (chờ 15s), rồi quay về bến.
 */
public class BoatFishingTest extends JPanel {

    private final World world;
    private final Human fisherman;
    private final Boat boat;
    private final Timer timer;
    private boolean isRunning = true;
    
    private final Camera camera;
    private final RenderSystem renderSystem;

    public BoatFishingTest() {
        world = new World();
        world.setWidth(800);
        world.setHeight(600);

        // Tạo GameMap giả lập bờ biển ở trên (y < 200), biển ở dưới (y >= 200)
        world.setGameMap(new model.map.GameMap(null) {
            @Override
            public boolean isPositionInWater(float x, float y) {
                return y >= 200; // Nước ở nửa dưới màn hình
            }
            @Override
            public boolean isWaterTile(float x, float y) {
                return y >= 200;
            }
            @Override
            public boolean isBridgeTile(float x, float y) { return false; }
            @Override
            public boolean isValidGroundSpawnPosition(float x, float y, float margin) { return !isPositionInWater(x,y); }
            @Override
            public int getCols() { return 800 / 32; }
            @Override
            public int getRows() { return 600 / 32; }
        });

        // Tạo Bến thuyền ở sát mép nước (y = 190)
        boat = new Boat(new Vector2(400, 190));
        world.getCoastalManager().addBoat(boat);
        world.addEntity(boat);

        // Tạo Ngư dân ở trên bờ (y = 100)
        fisherman = new Human(new Vector2(400, 100), Human.Variant.MALE, HumanRole.FISHERMAN, new Vector2(400, 100), 50.0f);
        
        // Gán chiến lược đi thuyền
        fisherman.setStrategy(new BoardBoatStrategy(boat));
        world.addEntity(fisherman);

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        // Vòng lặp game
        timer = new Timer(16, e -> {
            if (isRunning) {
                // Tăng tốc độ mô phỏng x5 để không phải chờ lâu (chờ câu cá mất 15s thực tế)
                float dt = 0.016f * 5f;
                world.update(dt);
                
                // Giảm tốc độ bơi của cá xuống (vì x5 dt làm cá bơi quá nhanh trên màn hình)
                for (Entity ent : world.getEntities()) {
                    if (ent instanceof model.living_beings.Fish) {
                        model.living_beings.Fish f = (model.living_beings.Fish) ent;
                        if (f.getBaseSpeed() > 15f) {
                            f.setBaseSpeed(10f);
                        }
                    }
                }

                // Đồng bộ vị trí người theo thuyền nếu đang trên thuyền
                if (boat.getPassengers().contains(fisherman)) {
                    fisherman.setPosition(boat.getPosition().copy());
                }

                repaint(); // Cập nhật màn hình
                
                // Điều kiện thắng: Thuyền đã đi câu xong, quay về bến, người xuống thuyền và có mang theo thức ăn
                if (fisherman.getCarriedFood() > 0 && boat.getState() == Boat.BoatState.DOCKED && !boat.getPassengers().contains(fisherman)) {
                    System.out.println("TEST PASSED: Fisherman successfully sailed, cast net, caught fish and returned to dock!");
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
        
        camera.setViewportSize(getWidth(), getHeight());
        
        // Vẽ nền: Bờ biển (cát) và Nước biển
        g2d.setColor(new Color(237, 201, 175)); // Màu cát
        g2d.fillRect(0, 0, getWidth(), 200);
        g2d.setColor(new Color(64, 164, 223)); // Màu nước biển
        g2d.fillRect(0, 200, getWidth(), 400);

        // Sử dụng hệ thống render của game
        renderSystem.renderAll(world, g2d, 0.016f);
        
        // Vẽ Text trạng thái thuyền và người
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Boat State: " + boat.getState(), 10, 20);
        g2d.drawString("Fisherman State: " + fisherman.getActionState(), 10, 40);
        g2d.drawString("Fisherman Carried Food: " + fisherman.getCarriedFood(), 10, 60);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test 2: Boat Fishing Action");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        BoatFishingTest panel = new BoatFishingTest();
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

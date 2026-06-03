package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.plants.Grass;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Kịch bản "Chướng ngại vật Di động" (Dynamic Obstacles).
 *
 * Mục đích: Kiểm tra khả năng Context Steering của Hươu khi vật cản KHÔNG đứng yên.
 * - 2 con Hươu đang đói, đang cố gắng tới ăn bụi cỏ ở bên phải.
 * - Một "đàn Voi" khổng lồ (5 con) đang đi ngang từ trên xuống dưới,
 *   cắt ngang đường đi của Hươu.
 * - Hươu phải liên tục thay đổi quỹ đạo để lách qua từng con Voi đang di chuyển.
 */
public class DynamicObstacleTest extends JPanel {

    /**
     * Voi "chặn đường" - được đánh dấu isSolid để Hươu nhận diện là vật cản,
     * và di chuyển theo một hướng cố định thay vì dùng AI của game.
     */
    static class HerdElephant extends Elephant {
        private final float speedX;
        private final float speedY;

        HerdElephant(Vector2 pos, float speedX, float speedY) {
            super(pos);
            this.setSolid(true); // Đánh dấu là vật cản cứng để Context Steering né
            this.speedX = speedX;
            this.speedY = speedY;
        }

        @Override
        public void update(float deltaTime) {
            // Ghi đè AI mặc định — chỉ đi thẳng theo hướng cố định
            if (position != null) {
                position.x += speedX * deltaTime;
                position.y += speedY * deltaTime;
            }
        }

        // Cho phép set solid từ bên ngoài
        public void setSolid(boolean v) { this.isSolid = v; }
    }

    private final World world;
    private final List<HerdElephant> elephants = new ArrayList<>();
    private final Timer timer;
    private boolean isRunning = true;

    private final Camera camera;
    private final RenderSystem renderSystem;

    // Kích thước bản đồ
    private static final int MAP_W = 700;
    private static final int MAP_H = 500;

    public DynamicObstacleTest() {
        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);

        // Tắt auto-spawn để không bị nhiễu
        model.world.PopulationManager.setEnabled(false);

        // 1. Đàn Voi đi từ trên xuống dưới, cắt ngang giữa bản đồ
        //    Xếp thành hàng dọc, cách nhau đều, di chuyển với tốc độ 60px/s
        int numElephants = 5;
        float elephantX = MAP_W / 2.0f; // Đường đi của đàn Voi ở giữa màn hình
        float startY = -50; // Bắt đầu từ trên (ngoài màn hình)
        for (int i = 0; i < numElephants; i++) {
            HerdElephant e = new HerdElephant(
                new Vector2(elephantX, startY - i * 120), // Cách nhau 120px
                0, 60 // Đi thẳng xuống với 60px/s
            );
            world.addEntity(e);
            elephants.add(e);
        }

        // 2. Bụi cỏ (mục tiêu của Hươu) ở phía bên phải
        world.addEntity(new Grass(new Vector2(MAP_W - 80, 150)));
        world.addEntity(new Grass(new Vector2(MAP_W - 80, 350)));

        // 3. Hươu xuất phát từ bên trái, đang rất đói → bắt buộc phải lao qua đàn Voi
        Deer deer1 = new Deer(new Vector2(60, 150));
        deer1.setHunger(5.0);
        world.addEntity(deer1);

        Deer deer2 = new Deer(new Vector2(60, 350));
        deer2.setHunger(5.0);
        world.addEntity(deer2);

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        timer = new Timer(16, e -> {
            if (isRunning) {
                world.update(0.016f);

                // Voi ra khỏi màn hình thì reset lại vị trí phía trên
                for (HerdElephant el : elephants) {
                    if (el.getPosition().y > MAP_H + 60) {
                        el.getPosition().y = -60;
                    }
                }

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

        // Vẽ đường đàn Voi đi (mũi tên chỉ hướng)
        g2d.setColor(new Color(180, 100, 30, 80));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            0, new float[]{8, 6}, 0));
        g2d.drawLine(MAP_W / 2, 0, MAP_W / 2, MAP_H);

        // Render tất cả entity
        renderSystem.renderAll(world, g2d, 0.016f);

        // Vẽ thanh máu cho các con vật
        for (Entity e : world.getEntities()) {
            if (e instanceof Animal && e.isAlive()) {
                Animal a = (Animal) e;
                float hpRatio = (float) (a.getHealth() / a.getMaxHealth());
                int barW = 36, barH = 5;
                int bx = (int) a.getPosition().x - barW / 2;
                int by = (int) a.getPosition().y - (int) a.getSize() / 2 - 12;

                g2d.setStroke(new BasicStroke(1));
                g2d.setColor(new Color(180, 30, 30));
                g2d.fillRoundRect(bx, by, barW, barH, 3, 3);
                g2d.setColor(new Color(60, 200, 60));
                g2d.fillRoundRect(bx, by, (int) (barW * hpRatio), barH, 3, 3);
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(bx, by, barW, barH, 3, 3);
            }
        }

        // Label góc trên trái
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2d.drawString("Đàn Voi (↓) cắt ngang đường đi của Hươu (→)", 10, 22);
        g2d.setColor(new Color(255, 230, 100));
        g2d.drawString("Hươu dùng Context Steering để lách qua Voi đang di chuyển", 10, 42);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Dynamic Obstacles — Đàn Voi Di Động");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DynamicObstacleTest panel = new DynamicObstacleTest();
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

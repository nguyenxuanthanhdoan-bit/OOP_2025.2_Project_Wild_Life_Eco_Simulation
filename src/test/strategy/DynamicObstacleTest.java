package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.plants.Grass;
import model.structures.Rock;
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
 * Kịch bản "Chướng ngại vật Di động" (Dynamic Obstacles) — Bản mở rộng.
 *
 * Mục đích: Kiểm tra Context Steering khi vật cản ĐANG DI CHUYỂN.
 * - Bản đồ lớn 900x700.
 * - 3 làn Voi đi song song từ trái qua phải, mỗi làn 3 con, cách đều nhau.
 * - 5 con Hươu đói xuất phát từ trên, bắt buộc phải đi xuống dưới để ăn cỏ.
 * - Hươu phải liên tục tính toán né tránh từng con Voi đang di chuyển.
 */
public class DynamicObstacleTest extends JPanel {

    /**
     * Voi chạy thẳng theo hướng cố định — hiện đầy đủ sprite và hoạt ảnh.
     */
    static class HerdElephant extends Elephant {
        private final float vx; // Vận tốc X
        private final float vy; // Vận tốc Y

        HerdElephant(Vector2 pos, float vx, float vy) {
            super(pos);
            this.vx = vx;
            this.vy = vy;
            this.isSolid = true; // Nhận diện là vật cản bởi Context Steering
        }

        @Override
        public void update(float deltaTime) {
            if (position == null) return;

            // Di chuyển thẳng theo hướng cố định
            position.x += vx * deltaTime;
            position.y += vy * deltaTime;

            // Cập nhật trạng thái để hoạt ảnh chạy đúng
            setActionState("walk");
            isMoving = true; // trực tiếp field protected của Animal
            if (vx > 0) setFacingRight(true);
            else if (vx < 0) setFacingRight(false);
        }
    }

    private static final int MAP_W = 900;
    private static final int MAP_H = 700;

    private final World world;
    private final List<HerdElephant> elephants = new ArrayList<>();
    private final Timer timer;
    private boolean isRunning = true;
    private float elapsed = 0f;

    private final Camera camera;
    private final RenderSystem renderSystem;

    public DynamicObstacleTest() {
        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);
        model.world.PopulationManager.setEnabled(false);

        // ── 1. ĐÀN VOI: 3 làn, mỗi làn 3 con, đi từ trái qua phải ──
        // Làn 1: y ≈ 200, tốc độ 55px/s
        // Làn 2: y ≈ 380, tốc độ 70px/s (nhanh hơn)
        // Làn 3: y ≈ 560, tốc độ 45px/s (chậm nhất)
        int[][] lanes = {
            {200, 55},
            {380, 70},
            {560, 45}
        };
        for (int[] lane : lanes) {
            int laneY = lane[0];
            int speed = lane[1];
            for (int j = 0; j < 3; j++) {
                // Mỗi con trong làn cách nhau 220px (vừa đủ khoảng lách)
                HerdElephant el = new HerdElephant(
                    new Vector2(-60 - j * 220, laneY),
                    speed, 0
                );
                world.addEntity(el);
                elephants.add(el);
            }
        }

        // ── 2. BỤI CỎ: mục tiêu của Hươu, rải ở khu vực phía dưới ──
        world.addEntity(new Grass(new Vector2(200, MAP_H - 80)));
        world.addEntity(new Grass(new Vector2(450, MAP_H - 80)));
        world.addEntity(new Grass(new Vector2(700, MAP_H - 80)));

        // ── 3. HƯƠU: 5 con xuất phát từ trên, phải đi xuống qua làn Voi ──
        int[] deerXs = {150, 300, 450, 600, 750};
        for (int x : deerXs) {
            Deer d = new Deer(new Vector2(x, 60));
            d.setHunger(4.0); // Rất đói → lao thẳng xuống dưới tìm cỏ
            world.addEntity(d);
        }

        // ── 4. MỘT VÀI TẢNG ĐÁ TĨnh làm phong phú địa hình ──
        world.addEntity(new Rock(new Vector2(350, 300)));
        world.addEntity(new Rock(new Vector2(550, 450)));

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        timer = new Timer(16, e -> {
            if (isRunning) {
                elapsed += 0.016f;
                world.update(0.016f);

                // Voi ra khỏi rìa phải → reset về rìa trái
                for (HerdElephant el : elephants) {
                    if (el.getPosition().x > MAP_W + 80) {
                        el.getPosition().x = -80;
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
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        camera.setViewportSize(getWidth(), getHeight());

        // ── NỀN ──
        // Cỏ xanh nhạt phía trên (khu Hươu)
        GradientPaint bgGrad = new GradientPaint(
            0, 0, new Color(100, 170, 90),
            0, getHeight(), new Color(60, 130, 55)
        );
        g2d.setPaint(bgGrad);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Kẻ đường làn Voi (nền màu nhạt hơn)
        int[][] lanes = {{180, 240}, {360, 420}, {540, 600}};
        for (int[] lane : lanes) {
            g2d.setColor(new Color(255, 220, 120, 40));
            g2d.fillRect(0, lane[0], getWidth(), lane[1] - lane[0]);

            // Đường kẻ làn — nét đứt
            g2d.setColor(new Color(220, 180, 60, 100));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{12, 8}, elapsed * 15));
            g2d.drawLine(0, lane[0], getWidth(), lane[0]);
            g2d.drawLine(0, lane[1], getWidth(), lane[1]);
        }

        // Viền khép kín
        g2d.setColor(new Color(60, 40, 20));
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);

        // ── RENDER ENTITY ──
        renderSystem.renderAll(world, g2d, 0.016f);

        // ── THANH MÁU & NHÃN TÊN ──
        g2d.setStroke(new BasicStroke(1));
        for (Entity e : world.getEntities()) {
            if (!(e instanceof Animal) || !e.isAlive()) continue;
            Animal a = (Animal) e;
            if (a.getPosition() == null) continue;

            int bx = (int) a.getPosition().x;
            int by = (int) a.getPosition().y;
            int sz = (int) a.getSize();

            // Nhãn tên
            if (a instanceof Deer) {
                g2d.setColor(new Color(255, 255, 100, 200));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2d.drawString("Hươu", bx - 12, by - sz / 2 - 14);
            } else if (a instanceof HerdElephant) {
                g2d.setColor(new Color(180, 230, 255, 200));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2d.drawString("Voi", bx - 8, by - sz / 2 - 14);
            }

            // Thanh máu
            float hpRatio = (float) (a.getHealth() / a.getMaxHealth());
            int barW = (a instanceof HerdElephant) ? 50 : 36;
            int barH = 5;
            int barX = bx - barW / 2;
            int barY = by - sz / 2 - 10;

            g2d.setColor(new Color(60, 60, 60, 180));
            g2d.fillRoundRect(barX - 1, barY - 1, barW + 2, barH + 2, 3, 3);
            g2d.setColor(new Color(200, 40, 40));
            g2d.fillRoundRect(barX, barY, barW, barH, 3, 3);
            g2d.setColor(new Color(60, 210, 80));
            g2d.fillRoundRect(barX, barY, (int) (barW * hpRatio), barH, 3, 3);
        }

        // ── HUD GÓC TRÊN TRÁI ──
        long deerAlive = world.getEntities().stream()
            .filter(e -> e instanceof Deer && e.isAlive()).count();

        g2d.setColor(new Color(0, 0, 0, 130));
        g2d.fillRoundRect(6, 6, 390, 70, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2d.drawString("Dynamic Obstacles — Voi đi ngang, Hươu lách xuống", 14, 25);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(255, 230, 100));
        g2d.drawString("Hươu còn sống: " + deerAlive + "/5  |  3 làn Voi di động (→)", 14, 44);
        g2d.setColor(new Color(150, 220, 255));
        g2d.drawString(String.format("Thời gian: %.1fs", elapsed), 14, 62);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Dynamic Obstacles — Đàn Voi Di Động (Bản mở rộng)");
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

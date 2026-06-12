package test.strategy;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.animal.Animal;
import model.living_beings.Deer;
import model.living_beings.Hunter;
import model.living_beings.Rabbit;
import model.living_beings.Tiger;
import model.structures.FoodStorage;
import model.structures.Rock;
import model.world.PopulationManager;
import model.world.World;
import view.systems.Camera;
import view.systems.render.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Kịch bản test: Thợ săn đi săn và mang thịt về kho.
 *
 * - 1 Hunter thuộc làng ở bên trái.
 * - 1 FoodStorage làm điểm nộp thịt.
 * - Nhiều con mồi: thỏ, hươu.
 * - 1 hổ để kiểm tra hunter không sợ predator và vẫn có thể chọn mục tiêu hợp lệ.
 */
public class HunterHuntTest extends JPanel {
    private static final int MAP_W = 1100;
    private static final int MAP_H = 760;

    private final World world;
    private final Hunter hunter;
    private final FoodStorage storage;
    private final Camera camera;
    private final RenderSystem renderSystem;
    private final Timer timer;
    private boolean isRunning = true;
    private float elapsed = 0.0f;

    public HunterHuntTest() {
        PopulationManager.setEnabled(false);

        world = new World();
        world.setWidth(MAP_W);
        world.setHeight(MAP_H);

        Vector2 villageCenter = new Vector2(180, MAP_H / 2.0f);
        storage = new FoodStorage(new Vector2(120, MAP_H / 2.0f));
        world.addEntity(storage);

        hunter = new Hunter(new Vector2(210, MAP_H / 2.0f), villageCenter, 260.0f);
        hunter.setHunger(hunter.getMaxHunger() * 0.45);
        hunter.setThirst(hunter.getMaxThirst());
        world.addEntity(hunter);

        spawnPrey();
        spawnTiger();
        spawnRocks();

        camera = new Camera(0, 0);
        renderSystem = new RenderSystem(camera);

        timer = new Timer(16, e -> {
            if (!isRunning) return;
            elapsed += 0.016f;
            world.update(0.016f);
            repaint();
        });
        timer.start();
    }

    private void spawnPrey() {
        for (int i = 0; i < 4; i++) {
            Rabbit rabbit = new Rabbit(new Vector2(520 + i * 45, 280 + (i % 2) * 55));
            rabbit.setHunger(rabbit.getMaxHunger());
            rabbit.setThirst(rabbit.getMaxThirst());
            world.addEntity(rabbit);
        }

        for (int i = 0; i < 3; i++) {
            Deer deer = new Deer(new Vector2(610 + i * 55, 450 + (i % 2) * 45), i + 1);
            deer.setHunger(deer.getMaxHunger());
            deer.setThirst(deer.getMaxThirst());
            world.addEntity(deer);
        }
    }

    private void spawnTiger() {
        Tiger tiger = new Tiger(new Vector2(870, 370));
        tiger.setHunger(tiger.getMaxHunger());
        tiger.setThirst(tiger.getMaxThirst());
        world.addEntity(tiger);
    }

    private void spawnRocks() {
        world.addEntity(new Rock(new Vector2(390, 260)));
        world.addEntity(new Rock(new Vector2(390, 500)));
        world.addEntity(new Rock(new Vector2(760, 260)));
        world.addEntity(new Rock(new Vector2(760, 520)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        camera.setViewportSize(getWidth(), getHeight());

        g2d.setColor(new Color(108, 164, 91));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        renderSystem.renderAll(world, g2d, 0.016f);
        drawHud(g2d);
    }

    private void drawHud(Graphics2D g2d) {
        long rabbits = countAlive(Rabbit.class);
        long deer = countAlive(Deer.class);
        long tiger = countAlive(Tiger.class);

        g2d.setColor(new Color(0, 0, 0, 165));
        g2d.fillRoundRect(12, 12, 430, 100, 10, 10);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Hunter Hunt Test", 24, 34);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.drawString(String.format("Time: %.1fs", elapsed), 24, 54);
        g2d.drawString(String.format("Hunter carried food: %.1f / %.1f",
                hunter.getCarriedFood(), hunter.getCarryCapacity()), 24, 72);
        g2d.drawString(String.format("FoodStorage: %.1f / %.1f",
                storage.getStoredFood(), storage.getCapacity()), 24, 90);
        g2d.drawString("Alive - Rabbit: " + rabbits + " | Deer: " + deer + " | Tiger: " + tiger, 240, 54);
        g2d.drawString("Ammo: " + hunter.getAmmo() + " / " + hunter.getMaxAmmo(), 240, 72);
        g2d.drawString("Expected: hunter shoots, collects meat, then deposits to storage.", 240, 90);
    }

    private long countAlive(Class<?> type) {
        return world.getEntities().stream()
                .filter(entity -> type.isInstance(entity) && entity.isAlive())
                .count();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test: Hunter Hunting And Food Storage");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        HunterHuntTest panel = new HunterHuntTest();
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

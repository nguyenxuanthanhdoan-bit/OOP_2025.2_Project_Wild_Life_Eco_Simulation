package screen;

import controller.Simulation;
import view.HUD;
import view.systems.Camera;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GameScreen extends JPanel {
    private final Simulation simulation;
    private final HUD hud;
    
    // FPS Counter
    private int fps = 0;
    private int fpsCounter = 0;
    private long lastFpsTime = System.currentTimeMillis();

    public GameScreen(Simulation simulation) {
        this.simulation = simulation;
        this.hud = new HUD();
        
        this.setFocusable(true);
        this.setLayout(null); // Absolute positioning cho các component UI con (như ToggleButton)

        // Xử lý click chọn con vật (Inspector)
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Camera camera = simulation.getCamera();
                core.Vector2 worldClick = camera.screenToWorld(new core.Vector2(e.getX(), e.getY()));

                model.entity.Entity nearest = null;
                float bestDist = Float.MAX_VALUE;
                float selectRadius = 40.0f; // radius in world coordinates

                List<model.entity.Entity> entities = new ArrayList<>(simulation.getWorld().getEntities());
                for (model.entity.Entity entity : entities) {
                    if (entity.isAlive()) { // Chỉ lấy các thực thể còn tồn tại
                        float dist = entity.getPosition().distanceTo(worldClick);
                        // Giới hạn chọn dựa trên kích thước
                        if (dist < bestDist && dist <= Math.max(selectRadius, entity.getSize() * 1.5f)) {
                            bestDist = dist;
                            nearest = entity;
                        }
                    }
                }

                simulation.getRenderSystem().setSelectedEntity(nearest);
                GameScreen.this.requestFocusInWindow();
            }
        });
    }

    public void addToggleButton(JButton toggleButton) {
        this.add(toggleButton);
        // Position it at the top right of the screen
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                toggleButton.setBounds(GameScreen.this.getWidth() - 60, 15, 45, 45);
            }
        });
        // Initial bounds (will be updated when added to frame)
        toggleButton.setBounds(800 - 60, 15, 45, 45);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Cập nhật FPS
        long now = System.currentTimeMillis();
        fpsCounter++;
        if (now - lastFpsTime >= 1000) {
            fps = fpsCounter;
            fpsCounter = 0;
            lastFpsTime = now;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 1. Render World (qua RenderSystem)
        simulation.render(g2d, 0.016f);

        // 2. Render HUD (vẽ overlay thông tin)
        hud.render(g2d, simulation, fps);
    }
}

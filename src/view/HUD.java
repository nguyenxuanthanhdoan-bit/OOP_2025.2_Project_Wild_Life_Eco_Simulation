package view;

import controller.Simulation;
import model.world.World;
import model.entity.Entity;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HUD {

    private Font font;
    
    public HUD() {
        font = new Font("Segoe UI", Font.BOLD, 14);
    }

    public void render(Graphics2D g2d, Simulation simulation, int fps) {
        World world = simulation.getWorld();
        if (world == null) return;

        int animalCount = 0;
        int plantCount = 0;
        List<Entity> list = new ArrayList<>(world.getEntities());
        for (Entity e : list) {
            if (e != null && e.isAlive()) {
                if (e instanceof model.living_beings.Animal) {
                    animalCount++;
                } else if (e instanceof model.plants.Plant || e instanceof model.items.FoodSource) {
                    plantCount++;
                }
            }
        }

        Rectangle clip = g2d.getClipBounds();
        int screenH = (clip != null) ? clip.height : 600;

        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getHeight() + 4;
        
        String[] texts = {
            "Ngày trong game: " + world.getGameDay(),
            "Thời tiết: " + world.getCurrentWeather().getName(),
            "FPS: " + fps,
            "Tổng số động vật: " + animalCount,
            "Số cây / mồi: " + plantCount
        };

        int x = 15;
        // Bắt đầu vẽ từ dưới cùng lên, chừa một khoảng lề 15px
        int yStats = screenH - (texts.length * lineHeight) - 10;

        // Draw shadow and text for readability
        for (String text : texts) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(text, x + 2, yStats + 2);
            // Text
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, x, yStats);
            yStats += lineHeight;
        }

        // Vị trí cho Inspector (vẫn giữ ở góc trên bên trái)
        int y = 25;

        // Draw debug info if needed (e.g. Inspector from Main.java)
        // (Thông tin chi tiết của con vật đã được chuyển sang Sidebar)
    }
}

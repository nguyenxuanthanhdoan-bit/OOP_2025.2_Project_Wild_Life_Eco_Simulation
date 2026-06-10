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
            "Mùa hiện tại: " + world.getCurrentSeason().getName(),
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
        model.living_beings.Animal animal = simulation.getRenderSystem().getSelectedAnimal();
        if (animal != null && animal.isAliveState()) {
            y += lineHeight; // Add some spacing
            
            g2d.setColor(new Color(255, 235, 60)); // Gold for selected
            g2d.drawString("Đang chọn:", x, y);
            y += lineHeight;
            
            g2d.setColor(Color.WHITE);
            String[] infoTexts = {
                "Loài: " + animal.getSpeciesName(),
                String.format("Tuổi: %.1f / %.1f (%s)", animal.getAge(), animal.getMaxAge(), animal.isAdult() ? "Trưởng thành" : "Trẻ con"),
                String.format("Máu: %.1f%%", (animal.getHealth() / animal.getMaxHealth()) * 100.0),
                String.format("Đói: %.1f%%", (animal.getHunger() / animal.getMaxHunger()) * 100.0),
                String.format("Khát: %.1f%%", (animal.getThirst() / animal.getMaxThirst()) * 100.0),
                "Hành động: " + animal.getActionState().toUpperCase()
            };
            
            for (String info : infoTexts) {
                // Shadow
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.drawString(info, x + 2, y + 2);
                // Text
                g2d.setColor(Color.WHITE);
                g2d.drawString(info, x, y);
                y += lineHeight;
            }
        }
    }
}

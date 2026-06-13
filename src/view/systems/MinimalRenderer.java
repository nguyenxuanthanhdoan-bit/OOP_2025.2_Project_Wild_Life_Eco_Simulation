package view.systems;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Rabbit;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.plants.Grass;
import model.plants.FruitTree;
import model.items.FireballProjectile;
import model.items.Meat;
import model.items.Carcass;
import model.living_beings.Fox;
import model.living_beings.Fish;
import model.living_beings.Human;
import model.living_beings.Hunter;
import model.plants.Mushroom;
import model.plants.Fruit;
import model.structures.Bush;
import model.structures.Rock;
import model.structures.House;
import model.structures.Well;
import model.structures.FoodStorage;
import model.structures.Boat;
import java.awt.*;

/**
 * Chế độ hiển thị tối giản: Sử dụng các khối hình học cơ bản để đại diện cho thực thể.
 * Giúp tối ưu hiệu năng và dễ dàng quan sát mật độ quần thể.
 */
public class MinimalRenderer {

    private Camera camera;

    public MinimalRenderer(Camera camera) {
        this.camera = camera;
    }

    /**
     * Vẽ một thực thể dưới dạng hình khối đơn giản.
     */
    public void renderEntity(Entity e, Graphics2D g2d) {
        if (e instanceof Animal && ((Animal) e).isHidden()) {
            return;
        }

        Vector2 screenPos = camera.worldToScreen(e.getPosition());
        float zoom = camera.getZoomLevel();
        int size = (int) (e.getSize() * zoom);

        // Thiết lập màu sắc và hình dáng dựa trên loại thực thể
        if (e instanceof FireballProjectile) {
            g2d.setColor(new Color(230, 35, 20));
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Rabbit) {
            g2d.setColor(new Color(100, 149, 237)); // Màu lam cho Thỏ
            g2d.fillRect((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Deer) {
            g2d.setColor(new Color(255, 165, 0)); // Màu cam cho Hươu
            // Hươu vẽ hình thoi để phân biệt với Thỏ
            int[] xPoints = {
                    (int) screenPos.x,
                    (int) screenPos.x - size / 2,
                    (int) screenPos.x,
                    (int) screenPos.x + size / 2
            };
            int[] yPoints = {
                    (int) screenPos.y - size / 2,
                    (int) screenPos.y,
                    (int) screenPos.y + size / 2,
                    (int) screenPos.y
            };
            g2d.fillPolygon(xPoints, yPoints, 4);
        }
        else if (e instanceof Elephant) {
            g2d.setColor(new Color(120, 120, 140)); // Màu xám cho Voi
            // Voi vẽ hình tròn to — lớn nhất hệ sinh thái
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Tiger) {
            g2d.setColor(new Color(235, 115, 20)); // Màu cam cho Hổ
            int[] xPoints = { (int) screenPos.x, (int) screenPos.x - size / 2, (int) screenPos.x, (int) screenPos.x + size / 2 };
            int[] yPoints = { (int) screenPos.y - size / 2, (int) screenPos.y, (int) screenPos.y + size / 2, (int) screenPos.y };
            g2d.fillPolygon(xPoints, yPoints, 4);
        }
        else if (e instanceof Wolf) {
            g2d.setColor(new Color(120, 130, 140)); // Màu xám cho Sói
            int[] xPoints = { (int) screenPos.x, (int) screenPos.x - size / 2, (int) screenPos.x, (int) screenPos.x + size / 2 };
            int[] yPoints = { (int) screenPos.y - size / 2, (int) screenPos.y, (int) screenPos.y + size / 2, (int) screenPos.y };
            g2d.fillPolygon(xPoints, yPoints, 4);
        }
        else if (e instanceof Grass) {
            g2d.setColor(new Color(50, 205, 50)); // Xanh lá tươi
            int dotSize = Math.max(2, size / 3);
            g2d.fillOval((int) screenPos.x - dotSize / 2, (int) screenPos.y - dotSize / 2, dotSize, dotSize);
        }
        else if (e instanceof FruitTree) {
            g2d.setColor(new Color(34, 139, 34)); // Xanh rừng đậm
            int[] xPoints = {
                    (int) screenPos.x,
                    (int) screenPos.x - size / 2,
                    (int) screenPos.x + size / 2
            };
            int[] yPoints = {
                    (int) screenPos.y - size / 2,
                    (int) screenPos.y + size / 2,
                    (int) screenPos.y + size / 2
            };
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
        else if (e instanceof Fox) {
            g2d.setColor(new Color(205, 133, 63)); // Nâu da cáo
            int[] xPoints = { (int) screenPos.x, (int) screenPos.x - size / 2, (int) screenPos.x + size / 2 };
            int[] yPoints = { (int) screenPos.y + size / 2, (int) screenPos.y - size / 2, (int) screenPos.y - size / 2 };
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
        else if (e instanceof Fish) {
            g2d.setColor(new Color(0, 206, 209)); // Xanh ngọc
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 4, size, size / 2);
        }
        else if (e instanceof Hunter) {
            g2d.setColor(new Color(139, 69, 19)); // Nâu đậm thợ săn
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Human) {
            g2d.setColor(new Color(255, 222, 173)); // Màu da người
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Bush) {
            g2d.setColor(new Color(0, 100, 0)); // Xanh rêu tối
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Rock) {
            g2d.setColor(new Color(169, 169, 169)); // Xám đá
            g2d.fillRoundRect((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size, 5, 5);
        }
        else if (e instanceof House) {
            g2d.setColor(new Color(160, 82, 45)); // Nâu gỗ sienna
            g2d.fillRect((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Well) {
            g2d.setColor(new Color(70, 130, 180)); // Xanh thép
            g2d.drawOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
            g2d.fillOval((int) screenPos.x - size / 4, (int) screenPos.y - size / 4, size / 2, size / 2);
        }
        else if (e instanceof FoodStorage) {
            g2d.setColor(new Color(218, 165, 32)); // Vàng nhạt
            g2d.fillRect((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);
        }
        else if (e instanceof Mushroom) {
            g2d.setColor(new Color(255, 69, 0)); // Đỏ cam nấm
            int dotSize = Math.max(3, size / 2);
            g2d.fillOval((int) screenPos.x - dotSize / 2, (int) screenPos.y - dotSize / 2, dotSize, dotSize);
        }
        else if (e instanceof Fruit) {
            g2d.setColor(new Color(220, 20, 60)); // Đỏ thẫm
            int dotSize = Math.max(2, size / 3);
            g2d.fillOval((int) screenPos.x - dotSize / 2, (int) screenPos.y - dotSize / 2, dotSize, dotSize);
        }
        else if (e instanceof Meat) {
            g2d.setColor(new Color(255, 105, 180)); // Hồng nhạt thịt tươi
            g2d.fillRect((int) screenPos.x - size / 4, (int) screenPos.y - size / 4, size / 2, size / 2);
        }
        else if (e instanceof Carcass) {
            g2d.setColor(new Color(139, 0, 0)); // Đỏ máu khô
            g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 4, size, size / 2);
        }
        else if (e instanceof Boat) {
            g2d.setColor(new Color(139, 69, 19)); // Nâu gỗ thuyền
            g2d.fillArc((int) screenPos.x - size / 2, (int) screenPos.y - size / 4, size, size / 2, 180, 180);
        }

        // Vẽ ID hoặc trạng thái (Dành cho việc Debug trong Phase 1)
        // g2d.setColor(Color.WHITE);
        // g2d.drawString(e.getId().toString().substring(0, 4), (int) screenPos.x, (int) screenPos.y);
    }

    /**
     * Vẽ nền địa hình tối giản.
     */
    public void renderBackground(Graphics2D g2d, float width, float height) {
        // Vẽ một lưới (Grid) đơn giản để người chơi cảm nhận được sự di chuyển khi zoom out
        g2d.setColor(new Color(40, 40, 40));
        g2d.fillRect(0, 0, (int)width, (int)height);

        g2d.setColor(new Color(60, 60, 60));
        int gridSize = 50;
        for (int x = 0; x < width; x += gridSize) {
            Vector2 start = camera.worldToScreen(new Vector2(x, 0));
            Vector2 end = camera.worldToScreen(new Vector2(x, height));
            g2d.drawLine((int)start.x, 0, (int)start.x, 2000); // Tạm thời vẽ dài ra
        }
    }
}

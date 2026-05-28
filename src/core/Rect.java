package core;

public class Rect {
    public float x, y, width, height;

    public Rect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Hàm cực kỳ quan trọng để kiểm tra thực thể có đứng trong vùng này không
    public boolean containsPoint(Vector2 point) {
        return point.x >= x && point.x <= x + width &&
                point.y >= y && point.y <= y + height;
    }

    // (Tùy chọn) Lấy tâm của vùng này để spawn thực thể
    public Vector2 getCenter() {
        return new Vector2(x + width / 2, y + height / 2);
    }
}
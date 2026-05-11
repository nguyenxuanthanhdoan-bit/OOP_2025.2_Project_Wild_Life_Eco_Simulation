package core;

public class Vector2 {

    public static final float EPSILON = 1e-5f;

    public float x;
    public float y;

    public Vector2() {
        this.x = 0.0f;
        this.y = 0.0f;
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // GETTERS & SETTERS (TỐI ƯU GC)

    public Vector2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2 set(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
        return this;
    }

    /**
     * Tạo ra một bản sao mới của vector này.
     * Dùng khi bạn cần tính toán mà KHÔNG muốn làm thay đổi vector gốc.
     */
    public Vector2 copy() {
        return new Vector2(this.x, this.y);
    }

    // TÍNH TOÁN KHOẢNG CÁCH & ĐỘ DÀI

    /**
     * Tính bình phương khoảng cách (Tối ưu hiệu năng vì không dùng Math.sqrt).
     * Rất tốt khi chỉ cần SO SÁNH khoảng cách (VD: distanceSquared < radius * radius).
     */
    public float distanceSquared(Vector2 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    public float distanceTo(Vector2 other) {
        return (float) Math.sqrt(distanceSquared(other));
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    // CÁC PHÉP TOÁN (MUTABLE - THAY ĐỔI TRỰC TIẾP VECTOR GỐC)

    /**
     * Chuẩn hóa vector về độ dài 1 (Vector chỉ hướng).
     */
    public Vector2 normalize() {
        float len = length();
        if (len > EPSILON) {
            this.x /= len;
            this.y /= len;
        } else {
            this.x = 0.0f;
            this.y = 0.0f;
        }
        return this; // Trả về chính nó để có thể chain phương thức
    }

    /**
     * Cộng thêm vector khác vào vector hiện tại.
     * LƯU Ý: Làm thay đổi giá trị của vector gọi hàm.
     */
    public Vector2 add(Vector2 other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    /**
     * Trừ vector hiện tại đi một vector khác.
     */
    public Vector2 subtract(Vector2 other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    /**
     * Nhân vector hiện tại với một hệ số (Scale).
     */
    public Vector2 scale(float factor) {
        this.x *= factor;
        this.y *= factor;
        return this;
    }

    /**
     * Linear Interpolation (Nội suy tuyến tính).
     * Cực kỳ hữu ích để làm Camera bám theo nhân vật mượt mà, hoặc đạn bay mượt.
     * @param target Vị trí đích đến
     * @param alpha Tốc độ nội suy (0.0 đến 1.0)
     */
    public Vector2 lerp(Vector2 target, float alpha) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.x += alpha * (target.x - this.x);
        this.y += alpha * (target.y - this.y);
        return this;
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}
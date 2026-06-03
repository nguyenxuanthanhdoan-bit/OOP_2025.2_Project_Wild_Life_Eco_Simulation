package model.plants;

import core.DisplayMode;
import core.Vector2;

/**
 * Quả rụng từ {@link FruitTree} — nguồn thức ăn cho các loài ăn tạp và voi.
 *
 * <p>Khác với {@link FruitTree} (cây không bị ăn), {@code Fruit} là thực thể
 * được sinh ra khi cây kết quả. Voi ({@code Elephant}) và các loài ăn tạp
 * có thể nhặt và ăn quả này.</p>
 *
 * <ul>
 *   <li>Dinh dưỡng cao hơn Grass (30 điểm vs 10 điểm).</li>
 *   <li>Hết thời gian tự phân hủy ({@code decayTimer}).</li>
 * </ul>
 */
public class Fruit extends Plant {

    /** Dinh dưỡng mặc định của một quả. */
    private static final float DEFAULT_NUTRITION = 50.0f;

    /** Kích thước hình học mặc định (pixel). */
    private static final float DEFAULT_SIZE = 10.0f;

    /** Thời gian tồn tại tối đa trước khi tự hủy (giây mô phỏng). */
    private static final float MAX_DECAY_TIME = 60.0f;

    /** Bộ đếm phân hủy — về 0 thì quả hỏng. */
    private float decayTimer;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Tạo một quả rụng tại vị trí cho trước.
     *
     * @param position Vị trí rơi xuống (thường gần gốc FruitTree)
     */
    public Fruit(Vector2 position) {
        super(position, DEFAULT_SIZE, DEFAULT_NUTRITION);
        this.decayTimer = MAX_DECAY_TIME;
        this.imageVariant = "Fruit_" + (new java.util.Random().nextInt(2) + 1);
    }

    /**
     * Constructor tuỳ chỉnh dinh dưỡng.
     *
     * @param position      Vị trí
     * @param nutritionValue Dinh dưỡng (điểm hunger phục hồi khi ăn)
     */
    public Fruit(Vector2 position, float nutritionValue) {
        super(position, DEFAULT_SIZE, nutritionValue);
        this.decayTimer = MAX_DECAY_TIME;
    }

    // =========================================================
    // UPDATE — Phân hủy theo thời gian
    // =========================================================

    /**
     * Quả tự phân hủy theo thời gian.
     * Khi {@code decayTimer} về 0, quả trở thành không ăn được ({@code alive = false}).
     *
     * @param deltaTime Thời gian frame (giây)
     */
    @Override
    public void update(float deltaTime) {
        if (!isAlive) return;
        decayTimer -= deltaTime;
        if (decayTimer <= 0) {
            this.isAlive = false;
        }
    }

    // =========================================================
    // RENDER
    // =========================================================

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem đảm nhận vẽ sprite quả
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public float getDecayTimer() { return decayTimer; }
    public void setDecayTimer(float decayTimer) { this.decayTimer = decayTimer; }

    @Override
    public String toString() {
        return String.format("Fruit[pos=%s | nutrition=%.1f | decay=%.1fs]",
                position, nutritionValue, decayTimer);
    }
}

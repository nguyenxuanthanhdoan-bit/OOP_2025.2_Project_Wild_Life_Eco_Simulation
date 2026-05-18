package model.living_beings;

import core.Vector2;

/**
 * Lớp cơ sở cho các loài động vật ăn cỏ.
 * Phiên bản Phase 1: Tối giản, chỉ làm cầu nối kế thừa từ Animal.
 */
public abstract class HerbivoreAnimal extends Animal {

    public HerbivoreAnimal(Vector2 position, float size, float baseSpeed) {
        super(position, size, baseSpeed);
    }

    // Các logic như:
    // - eatGrass() (Ăn cỏ)
    // - eatFruit() (Ăn quả rụng)
    // Sẽ được thêm vào khi hệ thống thực vật xuất hiện ở Phase sau.
}
package living_beings;

import core.Vector2;

/**
 * Lớp cơ sở cho các loài động vật hoang dã.
 * Phiên bản Phase 1: Tối giản, chỉ làm cầu nối kế thừa từ LivingBeing.
 */
public abstract class Animal extends LivingBeing {

    public Animal(Vector2 position, float size, float baseSpeed) {
        super(position, size, baseSpeed);
    }

    // Các logic phức tạp như:
    // - detectPredators() (Phát hiện kẻ thù)
    // - hideInBush() (Chui vào bụi rậm)
    // Sẽ được thêm vào ở các Phase sau.
}
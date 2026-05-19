package model.plants;

import core.Vector2;
import core.StructureType;
import model.entity.Entity;

public class Grass extends Plant {

    public Grass(Vector2 position) {
        // [ĐÃ SỬA] Thêm tham số `true` (isWalkable) vào để khớp với cấu trúc mới của Plant
        super(position, 15.0f, StructureType.GRASS, true, 10.0f);
    }

    @Override
    public void onInteract(Entity actor) {
        // Logic khi thỏ ăn cỏ sẽ viết ở đây.
        // Ví dụ: Báo cho World xóa cục cỏ này đi
    }

    @Override
    public void update(float deltaTime) {
        // Logic sinh sôi nảy nở của cỏ
    }
}
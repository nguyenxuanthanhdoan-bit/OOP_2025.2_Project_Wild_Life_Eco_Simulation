package model.structures;

import core.Vector2;
import core.StructureType;
import model.entity.Entity;
import model.entity.Structure;

public class FallenFruit extends Structure {

    private float nutritionValue;

    public FallenFruit(Vector2 position) {
        // Quả rụng nhỏ bé (8px), đi xuyên qua thoải mái (true)
        super(position, 8f, StructureType.FALLEN_FRUIT, true, false);
        this.nutritionValue = 15f; // Điểm hồi đói
    }

    @Override
    public void onInteract(Entity actor) {
        // Logic khi thỏ/người nhặt lên ăn [cite: 47, 48]
    }

    @Override
    public void update(float deltaTime) {
        // Lời nhắn cho người làm Logic:
        // Viết code đếm ngược thời gian thối rữa ở đây.
        // Hết hạn thì gọi: world.removeEntity(this); để tự xóa chính mình.
    }
}
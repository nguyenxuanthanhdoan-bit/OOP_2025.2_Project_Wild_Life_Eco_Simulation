package model.plants;

import core.Vector2;
import core.StructureType;
import model.entity.Entity;

public class FruitTree extends Plant {

    public FruitTree(Vector2 position) {
        // isWalkable = false: Thân cây cứng chặn đường Thỏ, Hươu, Sói
        // Dinh dưỡng = 0: Động vật ăn quả rụng chứ không gặm thân cây [cite: 44, 47]
        super(position, 32f, StructureType.FRUIT_TREE, false, 0f);
    }

    @Override
    public void onInteract(Entity actor) {
        // Thân cây vĩnh cửu không bị tương tác ăn mòn, nên để rỗng [cite: 43, 44]
    }

    @Override
    public void update(float deltaTime) {
        // Lời nhắn cho người làm Logic:
        // Viết code đếm thời gian (timer += deltaTime) ở đây.
        // Khi đủ thời gian, gọi lệnh: world.addEntity(new FallenFruit(tọa_độ_mới));
    }
}
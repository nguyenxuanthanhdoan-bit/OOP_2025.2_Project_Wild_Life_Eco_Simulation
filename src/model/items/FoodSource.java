package model.items;

import model.entity.Entity;
import core.Vector2;
import core.DisplayMode;

/**
 * Lớp cơ sở trừu tượng cho mọi nguồn thức ăn (Carcass, Meat, Fruit...)
 */
public abstract class FoodSource extends Entity {
    
    public FoodSource(Vector2 position, float size) {
        super(position, size);
        this.isSolid = false;
    }
    
    /**
     * Tiêu thụ (ăn) một lượng thức ăn.
     * @param amount Khối lượng cắn được.
     * @return Lượng dinh dưỡng thực tế nhận được.
     */
    public abstract float consume(float amount);
}

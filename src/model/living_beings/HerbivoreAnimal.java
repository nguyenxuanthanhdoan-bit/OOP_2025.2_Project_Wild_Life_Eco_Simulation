package model.living_beings;

import core.Vector2;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Plant;

/**
 * Lớp cơ sở trừu tượng cho <b>động vật ăn cỏ (Herbivore)</b>.
 *
 * <p>Cung cấp hành vi ăn mặc định cho Grass và Fruit.
 * Các lớp con ({@code Rabbit}, {@code Deer}, {@code Elephant}) override
 * {@link #eat(Plant)} nếu chỉ muốn ăn một tập con.</p>
 *
 * <h3>Cây kế thừa:</h3>
 * Entity → LivingBeing → Animal → HerbivoreAnimal
 */
public abstract class HerbivoreAnimal extends Animal {

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * @param position  Vị trí khởi tạo
     * @param size      Kích thước hình học
     * @param baseSpeed Tốc độ cơ bản (pixel/giây)
     */
    public HerbivoreAnimal(Vector2 position, float size, float baseSpeed) {
        super(position, size, baseSpeed, DietType.HERBIVORE);
    }


}
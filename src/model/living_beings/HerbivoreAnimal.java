package model.living_beings;

import core.Vector2;
import model.living_beings.animal.Animal;
import model.plants.Fruit;
import model.plants.Grass;
import model.plants.Mushroom;
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

    // =========================================================
    // PHƯƠNG THỨC ĂN ĐA HÌNH
    // =========================================================

    /**
     * Ăn thực vật.
     * HerbivoreAnimal chấp nhận {@link Grass} và {@link Fruit} theo mặc định.
     * Lớp con override để thu hẹp hoặc mở rộng danh sách thức ăn.
     *
     * @param food Thực vật muốn ăn
     */
    @Override
    public void eat(Plant food) {
        if (!alive || food == null || !food.isAlive()) return;
        if (!canEatPlant(food)) return;

        if (food instanceof Grass) {
            eatGrass((Grass) food);
        } else if (food instanceof Fruit) {
            eatFruit((Fruit) food);
        } else if (food instanceof Mushroom) {
            eatPlant(food);
        }
        // Các loại Plant khác bị bỏ qua mặc định
    }

    @Override
    public boolean canEatPlant(Plant food) {
        return getProfile().canEatPlant(food);
    }

    /**
     * Ăn Grass — tăng hunger theo {@code nutritionValue}.
     *
     * @param grass Cỏ muốn ăn (không null, phải còn sống)
     */
    public void eatGrass(Grass grass) {
        if (!alive || grass == null || !grass.isAlive()) return;
        this.setHunger(Math.min(this.getMaxHunger(), this.getHunger() + grass.getNutritionValue()));
        grass.setAlive(false);
    }

    /**
     * Ăn Fruit — dinh dưỡng cao hơn Grass.
     * Mặc định trong HerbivoreAnimal là được phép ăn quả;
     * Rabbit override để chặn hành vi này.
     *
     * @param fruit Quả rụng muốn ăn (không null, phải còn sống)
     */
    public void eatFruit(Fruit fruit) {
        if (!alive || fruit == null || !fruit.isAlive()) return;
        this.setHunger(Math.min(this.getMaxHunger(), this.getHunger() + fruit.getNutritionValue()));
        fruit.setAlive(false);
    }

    protected void eatPlant(Plant plant) {
        if (!alive || plant == null || !plant.isAlive()) return;
        this.setHunger(Math.min(this.getMaxHunger(), this.getHunger() + plant.getNutritionValue()));
        plant.setAlive(false);
    }
}

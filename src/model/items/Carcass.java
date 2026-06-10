package model.items;

import core.Vector2;
import core.DisplayMode;
import model.world.World;

/**
 * Lớp trừu tượng quản lý xác động vật.
 */
public class Carcass extends FoodSource {
    protected float nutritionValue;
    protected float decayTimer;
    protected float currentMass;
    protected float initialMass;
    protected float initialSize;
    protected String sourceSpecies;
    private final boolean humanSource;
    
    private World world; // Cần để rớt xương khi phân hủy

    public Carcass(Vector2 position, float size, float nutritionValue, float decayTime, float initialMass, String sourceSpecies) {
        this(position, size, nutritionValue, decayTime, initialMass, sourceSpecies, false);
    }

    public Carcass(Vector2 position, float size, float nutritionValue, float decayTime,
                   float initialMass, String sourceSpecies, boolean humanSource) {
        super(position, size);
        this.nutritionValue = nutritionValue;
        this.decayTimer = decayTime;
        this.currentMass = initialMass;
        this.initialMass = initialMass;
        this.initialSize = size;
        this.sourceSpecies = sourceSpecies;
        this.humanSource = humanSource;
        this.imageVariant = "meat";
    }

    /** Gắn tham chiếu world để Carcass có thể rớt xương khi phân hủy. */
    public void setWorld(World world) {
        this.world = world;
    }

    public String getSourceSpecies() {
        return sourceSpecies;
    }

    public boolean isHumanSource() {
        return humanSource;
    }

    public float getCurrentMass() {
        return currentMass;
    }

    @Override
    public float consume(float amount) {
        if (!isAlive || currentMass <= 0) return 0;

        float consumed = Math.min(amount, currentMass);
        currentMass -= consumed;

        if (currentMass <= 0) {
            this.isAlive = false; // Bị ăn hết, xóa khỏi thế giới
        } else {
            float massRatio = currentMass / initialMass;
            this.size = Math.max(initialSize * 0.35f, initialSize * (float) Math.sqrt(massRatio));
        }

        return consumed * (nutritionValue / initialMass);
    }

    @Override
    public void update(float deltaTime) {
        if (!isAlive) return;
        decayTimer -= deltaTime;
        if (decayTimer <= 0) {
            this.isAlive = false;
            // Rớt xương khi xác phân hủy tự nhiên
            if (world != null && this.position != null) {
                world.addEntity(new Bone(this.position.copy()));
            }
        }
    }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ dùng imageVariant để vẽ
    }
}

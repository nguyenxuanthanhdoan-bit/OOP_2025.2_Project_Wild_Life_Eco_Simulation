package model.entity;

import core.Vector2;
import core.DisplayMode;
import java.util.UUID;

/**
 * Lớp cơ sở trừu tượng cho mọi thực thể trong hệ thống.
 * Đã được tối giản cho Phase 1.
 */
public abstract class Entity {

    protected final UUID id;
    protected Vector2 position;
    protected float size;
    protected boolean isAlive;
    protected model.world.World world;
    protected boolean isSolid = false;
    protected String imageVariant = "";

    public Entity(Vector2 position, float size) {
        this.id = UUID.randomUUID();
        // Dùng copy() để tránh trỏ nhầm tham chiếu đến Vector2 bên ngoài
        this.position = position.copy();
        this.size = size;
        this.isAlive = true;
    }

    public UUID getId() {
        return this.id;
    }

    public Vector2 getPosition() {
        return this.position;
    }

    public void setPosition(Vector2 pos) {
        // Dùng hàm set() của Vector2 để thay đổi giá trị thay vì tạo object mới
        this.position.set(pos);
    }

    public float getSize() {
        return this.size;
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    public model.world.World getWorld() {
        return this.world;
    }

    public void setWorld(model.world.World world) {
        this.world = world;
    }

    public boolean isSolid() {
        return this.isSolid;
    }

    public void setSolid(boolean solid) {
        this.isSolid = solid;
    }

    public String getImageVariant() {
        return this.imageVariant;
    }

    public void setImageVariant(String variant) {
        this.imageVariant = variant;
    }

    // ==========================================
    // ABSTRACT METHODS (Các lớp con phải tự định nghĩa)
    // ==========================================

    public abstract void update(float deltaTime);

    public abstract void render(DisplayMode mode);
}
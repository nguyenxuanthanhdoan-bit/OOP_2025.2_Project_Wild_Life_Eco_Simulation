package model.living_beings;

import core.Vector2;
import model.entity.Entity;

/**
 * Lớp cơ sở cho các sinh vật sống.
 * Phiên bản Phase 1: Tối giản hoàn toàn, chỉ chứa thuộc tính tốc độ để phục vụ di chuyển.
 */
public abstract class LivingBeing extends Entity {

    protected float speed;
    protected float baseSpeed;
    protected boolean facingRight = true;
    
    // Thuộc tính vật lý cho Steering Behavior
    protected Vector2 currentVelocity = new Vector2(0,0);
    protected float maxAcceleration = 300.0f; // Mặc định

    public LivingBeing(Vector2 position, float size, float baseSpeed) {
        super(position, size);
        this.baseSpeed = baseSpeed;
        this.speed = baseSpeed;
        this.collider = new model.collision.Collider(this, size * 0.4f, model.collision.CollisionLayer.ANIMAL);
    }

    /**
     * Hỗ trợ di chuyển cơ bản cho mọi sinh vật.
     * Vị trí mới = Vị trí cũ + (Hướng * Tốc độ * DeltaTime)
     * * @param direction Hướng di chuyển (Bắt buộc phải là Vector2 đã được normalize)
     * @param deltaTime Thời gian trôi qua giữa 2 frame
     */
    public void move(Vector2 direction, float deltaTime) {
        if (!this.isAlive) return;

        // Vận tốc mong muốn (dựa trên bộ não AI quyết định)
        Vector2 desiredVelocity = direction.copy().scale(this.speed);

        // Tính toán lực Steering
        Vector2 steering = model.strategies.SteeringBehavior.calculateSteering(this.currentVelocity, desiredVelocity, this.maxAcceleration);
        
        // Cập nhật vận tốc hiện tại
        this.currentVelocity.add(steering.scale(deltaTime));
        
        // Giới hạn tốc độ không vượt quá speed
        if (this.currentVelocity.lengthSquared() > this.speed * this.speed) {
            this.currentVelocity.normalize().scale(this.speed);
        }

        Vector2 nextPosition = this.position.copy().add(this.currentVelocity.copy().scale(deltaTime));

        // Cộng vào nếu vị trí hợp lệ (biên bản đồ, nước)
        if (this.world != null && this.world.isValidPositionFor(this, nextPosition)) {
            this.position.set(nextPosition);
            
            // Tự động xử lý trượt khi va chạm chướng ngại vật (Cây, Đá, Bụi)
            model.collision.CollisionManager.resolveCollisions(this, this.world);
        } else {
            // Nếu bị chặn bởi viền hoặc nước
            this.currentVelocity.set(0, 0);
            
            if (this.currentStrategy instanceof model.strategies.PassiveStrategy) {
                ((model.strategies.PassiveStrategy) this.currentStrategy).forceStateChange();
            } else if (this.world == null) {
                // Dự phòng nếu world chưa kịp gắn
                this.position.set(nextPosition);
            }
        }
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getBaseSpeed() {
        return this.baseSpeed;
    }

    public void setBaseSpeed(float baseSpeed) {
        this.baseSpeed = baseSpeed;
    }
    // Trong src/model.living_beings/LivingBeing.java
    protected model.strategies.IStrategy currentStrategy;

    public void setStrategy(model.strategies.IStrategy strategy) {
        this.currentStrategy = strategy;
    }


    // Thêm hàm Getter để RenderSystem đọc được
    public boolean isFacingRight() {
        return facingRight;
    }
    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }
    @Override
    public void update(float deltaTime) {
        if (!isAlive || currentStrategy == null) return;
        // Ủy quyền toàn bộ logic hành động cho Strategy
        currentStrategy.execute(this, this.world, deltaTime);
    }
}
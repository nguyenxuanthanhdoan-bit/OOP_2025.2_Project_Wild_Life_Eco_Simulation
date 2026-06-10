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
        
        // Cập nhật hướng quay mặt
        if (this.currentVelocity.x > 0.1f) {
            this.facingRight = true;
        } else if (this.currentVelocity.x < -0.1f) {
            this.facingRight = false;
        }
        
        // Giới hạn tốc độ không vượt quá speed
        if (this.currentVelocity.lengthSquared() > this.speed * this.speed) {
            this.currentVelocity.normalize().scale(this.speed);
        }

        Vector2 nextPosition = this.position.copy().add(this.currentVelocity.copy().scale(deltaTime));

        if (this.world != null && this.world.isValidPositionFor(this, nextPosition)) {
            // Di chuyển thẳng ok
            this.position.set(nextPosition);
            // Xử lý trượt khi va chạm chướng ngại vật (Cây, Đá, Bụi)
            model.collision.CollisionManager.resolveCollisions(this, this.world);
        } else if (this.world != null) {
            // [TRƯỢT QUANH VẬT CẢN] Tách trục X và Y để lách qua góc cây
            Vector2 nextX = new Vector2(nextPosition.x, this.position.y);
            Vector2 nextY = new Vector2(this.position.x, nextPosition.y);

            boolean canX = this.world.isValidPositionFor(this, nextX);
            boolean canY = this.world.isValidPositionFor(this, nextY);

            if (canX) {
                this.position.set(nextX);
                this.currentVelocity.y = 0;
            } else if (canY) {
                this.position.set(nextY);
                this.currentVelocity.x = 0;
            } else {
                // [TRƯỢT TIẾP TUYẾN] Thử vuông góc với hướng đi
                Vector2 tangent1 = new Vector2(-direction.y, direction.x).scale(this.speed * deltaTime);
                Vector2 tangent2 = new Vector2(direction.y, -direction.x).scale(this.speed * deltaTime);
                Vector2 nextT1 = this.position.copy().add(tangent1);
                Vector2 nextT2 = this.position.copy().add(tangent2);

                if (this.world.isValidPositionFor(this, nextT1)) {
                    this.position.set(nextT1);
                } else if (this.world.isValidPositionFor(this, nextT2)) {
                    this.position.set(nextT2);
                } else {
                    // [MICRO-STEP] Kẹt cứng — rung lắc nhỏ để thoát
                    this.currentVelocity.set(0, 0);
                    if (this.currentStrategy instanceof model.strategies.PassiveStrategy) {
                        ((model.strategies.PassiveStrategy) this.currentStrategy).forceStateChange();
                    } else {
                        float rx = (float)(Math.random() * 10 - 5);
                        float ry = (float)(Math.random() * 10 - 5);
                        Vector2 randomStep = new Vector2(this.position.x + rx, this.position.y + ry);
                        if (this.world.isValidPositionFor(this, randomStep)) {
                            this.position.set(randomStep);
                        }
                    }
                }
            }
        } else {
            this.currentVelocity.set(0, 0);
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

    public Vector2 getCurrentVelocity() {
        return this.currentVelocity.copy();
    }

    public void setBaseSpeed(float baseSpeed) {
        this.baseSpeed = baseSpeed;
    }
    // Trong src/model.living_beings/LivingBeing.java
    protected model.strategies.IStrategy currentStrategy;

    public void setStrategy(model.strategies.IStrategy strategy) {
        if (strategy == this.currentStrategy) return; // tránh reset strategy đang chạy
        if (this.currentStrategy != null) {
            this.currentStrategy.onExit(this, this.world);
        }
        this.currentStrategy = strategy;
        if (this.currentStrategy != null) {
            this.currentStrategy.onEnter(this, this.world);
        }
    }

    public model.strategies.IStrategy getCurrentStrategy() {
        return this.currentStrategy;
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

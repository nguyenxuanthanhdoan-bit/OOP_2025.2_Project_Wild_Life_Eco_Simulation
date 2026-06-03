package model.strategies;

import core.Vector2;

/**
 * Lớp chịu trách nhiệm tính toán lực vật lý (Steering) để
 * chuyển động mềm mại hơn, chống rung lắc (jittering).
 */
public class SteeringBehavior {
    
    /**
     * Tính toán lực Steering (gia tốc) để chuyển dần từ vận tốc hiện tại
     * sang vận tốc mong muốn (desiredVelocity), giới hạn bởi maxAcceleration.
     */
    public static Vector2 calculateSteering(Vector2 currentVelocity, Vector2 desiredVelocity, float maxAcceleration) {
        Vector2 steering = desiredVelocity.copy().subtract(currentVelocity);
        if (steering.lengthSquared() > maxAcceleration * maxAcceleration) {
            steering.normalize().scale(maxAcceleration);
        }
        return steering;
    }

    /**
     * Tính toán lực đẩy ra xa các động vật khác để tránh chồng chéo (Separation).
     */
    public static Vector2 calculateSeparation(model.living_beings.LivingBeing owner, model.world.World world) {
        Vector2 force = new Vector2();
        if (world == null || world.getSpatialGrid() == null) return force;

        float separationRadius = owner.getSize() * 1.5f;
        java.util.List<model.entity.Entity> neighbors = world.getSpatialGrid().getNeighbors(owner.getPosition(), separationRadius);

        int count = 0;
        for (model.entity.Entity neighbor : neighbors) {
            if (neighbor != owner && neighbor instanceof model.living_beings.Animal && neighbor.isAlive()) {
                float dist = owner.getPosition().distanceTo(neighbor.getPosition());
                if (dist > 0 && dist < separationRadius) {
                    Vector2 push = owner.getPosition().copy().subtract(neighbor.getPosition());
                    push.normalize().scale(separationRadius - dist); // Càng gần đẩy càng mạnh
                    force.add(push);
                    count++;
                }
            }
        }

        if (count > 0) {
            force.scale(1.0f / count);
        }
        return force;
    }

    /**
     * Lực né vật cản tĩnh (Rock, House, Tree). Dựa trên hướng di chuyển mong muốn (desiredVelocity).
     */
    public static Vector2 calculateObstacleAvoidance(model.living_beings.LivingBeing owner, model.world.World world, Vector2 desiredVelocity) {
        Vector2 force = new Vector2();
        if (world == null || world.getSpatialGrid() == null || desiredVelocity.lengthSquared() == 0) return force;

        float lookAheadDist = owner.getSize() * 2.5f; // Khoảng cách nhìn trước
        java.util.List<model.entity.Entity> neighbors = world.getSpatialGrid().getNeighbors(owner.getPosition(), lookAheadDist);
        
        Vector2 forward = desiredVelocity.copy().normalize();

        for (model.entity.Entity neighbor : neighbors) {
            if (neighbor != owner && neighbor.getCollider() != null && neighbor.getCollider().getLayer() == model.collision.CollisionLayer.OBSTACLE) {
                // Bụi cây không cản đường, cho phép đi xuyên qua
                if (neighbor instanceof model.structures.Bush) {
                    continue;
                }

                // Tăng nhẹ bán kính né tránh để tránh đụng gốc cây
                float avoidRadius = owner.getCollider().getRadius() + neighbor.getCollider().getRadius() + owner.getSize() * 0.5f;
                
                Vector2 toObstacle = neighbor.getPosition().copy().subtract(owner.getPosition());
                float distanceToObstacle = toObstacle.length();

                // Chỉ quan tâm nếu vật cản ở phía trước (góc < 90 độ)
                if (forward.dot(toObstacle) > 0 && distanceToObstacle < lookAheadDist) {
                    // Chiếu toObstacle lên hướng di chuyển
                    float projLength = toObstacle.dot(forward);
                    Vector2 projVector = forward.copy().scale(projLength);
                    
                    // Khoảng cách từ tâm vật cản đến trục đường đi của con vật
                    Vector2 perpendicularVector = toObstacle.copy().subtract(projVector);
                    float distToLine = perpendicularVector.length();

                    // Nếu đường đi cắt ngang qua bán kính vật cản
                    if (distToLine < avoidRadius) {
                        // Tạo lực đẩy ngang (Lateral Force) ngược lại với perpendicularVector
                        Vector2 avoidForce;
                        if (distToLine > 0.01f) {
                            avoidForce = perpendicularVector.copy().scale(-1).normalize();
                        } else {
                            // Trực diện: sinh lực đẩy ngang tùy ý (trái/phải)
                            avoidForce = new Vector2(-forward.y, forward.x); 
                        }
                        
                        // Càng gần vật cản, lực đẩy ngang càng mạnh (gấp 5 lần tốc độ để quẹo gắt)
                        float multiplier = 1.0f + (lookAheadDist - distanceToObstacle) / lookAheadDist;
                        avoidForce.scale(owner.getSpeed() * 5.0f * multiplier);
                        
                        // Thêm 1 chút lực phanh/đẩy lùi
                        avoidForce.add(forward.copy().scale(-owner.getSpeed() * 2.0f));

                        force.add(avoidForce);
                    }
                }
            }
        }
        return force;
    }

    /**
     * Lực đẩy vào trong khi đến gần mép bản đồ hoặc mép nước.
     */
    public static Vector2 calculateBoundaryAvoidance(model.living_beings.LivingBeing owner, model.world.World world) {
        Vector2 force = new Vector2();
        if (world == null) return force;

        float margin = owner.getSize() * 1.5f;
        float x = owner.getPosition().x;
        float y = owner.getPosition().y;

        if (x < margin) force.x = owner.getSpeed();
        else if (x > world.getWidth() - margin) force.x = -owner.getSpeed();

        if (y < margin) force.y = owner.getSpeed();
        else if (y > world.getHeight() - margin) force.y = -owner.getSpeed();

        return force;
    }
}

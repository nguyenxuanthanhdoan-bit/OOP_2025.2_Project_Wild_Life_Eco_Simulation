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
}

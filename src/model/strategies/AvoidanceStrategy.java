package model.strategies;

import core.Vector2;
import model.living_beings.Animal;
import model.world.World;
import model.entity.Entity;
import java.util.List;

/**
 * Lớp chịu trách nhiệm sinh ra lực né tránh các động vật khổng lồ.
 */
public class AvoidanceStrategy {
    private static final float SIZE_THRESHOLD = 1.5f;

    public static Vector2 getAvoidanceForce(Animal owner, World world) {
        Vector2 avoidanceForce = new Vector2();
        if (world == null || world.getSpatialGrid() == null) return avoidanceForce;
        
        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(owner.getPosition(), (float) owner.getVisionRange());
        
        for (Entity e : neighbors) {
            if (e instanceof Animal && e != owner && e.isAlive()) {
                Animal other = (Animal) e;
                // Nếu kẻ kia lớn hơn đáng kể
                if (other.getSize() > owner.getSize() * SIZE_THRESHOLD) {
                    Vector2 diff = owner.getPosition().copy().subtract(other.getPosition());
                    float dist = diff.length();
                    
                    // Khoảng cách càng gần, lực né càng mạnh
                    float safeDist = owner.getSize() + other.getSize() + 50.0f;
                    
                    if (dist > 0 && dist < safeDist) {
                        // Tính vector vuông góc (né ngang)
                        Vector2 perpendicular = new Vector2(-diff.y, diff.x).normalize();
                        
                        // Hệ số sức mạnh của lực né (0 -> 1)
                        float strength = (safeDist - dist) / safeDist * 2.0f;
                        avoidanceForce.add(perpendicular.scale(strength));
                        
                        // Thêm 1 chút lực đẩy lùi (cách xa ra)
                        diff.normalize();
                        avoidanceForce.add(diff.scale(strength * 0.5f));
                    }
                }
            }
        }
        
        return avoidanceForce;
    }
}

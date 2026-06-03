package model.collision;

import core.Vector2;
import model.entity.Entity;
import model.world.World;
import model.living_beings.Animal;
import model.structures.Bush;

import java.util.List;

public class CollisionManager {

    /**
     * Resolves collisions for the given entity by adjusting its position to slide along obstacles.
     * Call this AFTER setting the entity's new position.
     */
    public static void resolveCollisions(Entity entity, World world) {
        if (entity.getCollider() == null || world == null || world.getSpatialGrid() == null) {
            return;
        }

        // Chỉ xét động vật đi đụng tường (ANIMAL vs OBSTACLE)
        if (entity.getCollider().getLayer() != CollisionLayer.ANIMAL) {
            return;
        }

        float checkRadius = entity.getCollider().getRadius() + 100.0f; // Bán kính tìm kiếm
        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(entity.getPosition(), checkRadius);

        for (Entity neighbor : neighbors) {
            if (neighbor == entity || !neighbor.isAlive() || neighbor.getCollider() == null) {
                continue;
            }

            if (!shouldCollide(entity, neighbor)) {
                continue;
            }

            // Circle vs Circle collision
            float dist = entity.getPosition().distanceTo(neighbor.getPosition());
            float minDist = entity.getCollider().getRadius() + neighbor.getCollider().getRadius();

            if (dist < minDist) {
                // Đã lún vào nhau
                float overlap = minDist - dist;
                Vector2 normal = entity.getPosition().copy().subtract(neighbor.getPosition());
                
                if (normal.lengthSquared() > 0) {
                    normal.normalize();
                } else {
                    // Nếu trùng tâm hoàn toàn, đẩy ngẫu nhiên ra ngoài
                    normal.set(1, 0); 
                }
                
                // Đẩy entity ra ngoài mép vật cản (Sliding effect)
                entity.getPosition().add(normal.scale(overlap));
            }
        }
    }

    private static boolean shouldCollide(Entity e1, Entity e2) {
        Collider c1 = e1.getCollider();
        Collider c2 = e2.getCollider();

        // Trong hệ thống này, ANIMAL chỉ kẹt khi đụng OBSTACLE
        if (c1.getLayer() == CollisionLayer.ANIMAL && c2.getLayer() == CollisionLayer.OBSTACLE) {
            // Ngoại lệ: Nếu thú đang nấp trong bụi rậm thì không bị đẩy ra
            if (e1 instanceof Animal && e2 instanceof Bush) {
                if (((Animal) e1).isHidden()) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}

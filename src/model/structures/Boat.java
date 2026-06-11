package model.structures;

import core.Vector2;
import model.entity.Structure;
import model.living_beings.Human;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Thuyền đánh cá — có thể chứa tối đa 2 người và di chuyển ra biển thả lưới.
 */
public class Boat extends Structure {
    public static final float BOAT_SIZE = 56.0f;
    public static final float DOCK_RADIUS = 80.0f;
    private static final int CAPACITY = 2;

    public enum BoatState {
        DOCKED,
        BOARDING,
        SAILING_OUT,
        FISHING,
        SAILING_BACK
    }

    private BoatState state = BoatState.DOCKED;
    private final List<Human> passengers = new ArrayList<>();
    private final Set<UUID> reservations = new HashSet<>();
    private final Vector2 dockPosition;
    private Vector2 fishingSpot;
    private float timer = 0f;
    private Random rand = new Random();

    public Boat(Vector2 position) {
        super(position, BOAT_SIZE, "BOAT", "boat", false);
        this.dockPosition = position.copy();
    }

    public BoatState getState() { return state; }
    public List<Human> getPassengers() { return passengers; }
    public Vector2 getDockPosition() { return dockPosition.copy(); }

    public boolean canBoard() {
        return canAcceptPassengers() && passengers.size() + reservations.size() < CAPACITY;
    }

    public boolean reserveSeat(Human human) {
        if (human == null || passengers.contains(human)) return false;
        if (reservations.contains(human.getId())) return true;
        if (!canBoard()) return false;
        return reservations.add(human.getId());
    }

    public void releaseReservation(Human human) {
        if (human != null) reservations.remove(human.getId());
    }

    public boolean isReservedBy(Human human) {
        return human != null && reservations.contains(human.getId());
    }

    public boolean board(Human human) {
        if (human == null || passengers.contains(human) || !canAcceptPassengers()) return false;
        if (!isReservedBy(human) && passengers.size() + reservations.size() >= CAPACITY) return false;
        reservations.remove(human.getId());
        passengers.add(human);
        state = BoatState.BOARDING;
        timer = 10f;
        return true;
    }
    
    public void unboard(Human human) {
        passengers.remove(human);
        releaseReservation(human);
    }

    private boolean canAcceptPassengers() {
        return state == BoatState.DOCKED || state == BoatState.BOARDING;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        switch (state) {
            case BOARDING:
                timer -= deltaTime;
                if (passengers.size() == CAPACITY || (timer <= 0 && reservations.isEmpty())) {
                    state = BoatState.SAILING_OUT;
                    fishingSpot = findFishingSpot();
                }
                break;
            case SAILING_OUT:
                if (fishingSpot != null) {
                    moveTo(fishingSpot, 40f, deltaTime); // speed 40
                    
                    // Ngăn thuyền đi lên cạn: nếu rời bến đủ xa mà lại ở trên cạn thì quay về
                    if (position.distanceTo(dockPosition) > 60f && getWorld() != null && getWorld().getGameMap() != null 
                            && !getWorld().getGameMap().isPositionInWater(position.x, position.y)) {
                        state = BoatState.SAILING_BACK;
                        fishingSpot = null;
                        break;
                    }

                    if (position.distanceTo(fishingSpot) < 5f) {
                        state = BoatState.FISHING;
                        timer = 15f; // Đánh lưới trong 15 giây
                    }
                } else {
                    state = BoatState.SAILING_BACK;
                }
                break;
            case FISHING:
                // Đảm bảo không thả lưới trên cạn
                if (getWorld() != null && getWorld().getGameMap() != null 
                        && !getWorld().getGameMap().isPositionInWater(position.x, position.y)) {
                    state = BoatState.SAILING_BACK;
                    break;
                }

                timer -= deltaTime;
                if (timer <= 0) {
                    for (Human h : passengers) {
                        h.addCarriedFood(25f); // Mỗi người 25 food
                    }
                    state = BoatState.SAILING_BACK;
                }
                break;
            case SAILING_BACK:
                moveTo(dockPosition, 40f, deltaTime);
                if (position.distanceTo(dockPosition) < 5f) {
                    state = BoatState.DOCKED;
                }
                break;
            case DOCKED:
                break;
        }
        
        // Sync passenger positions
        int idx = 0;
        for (Human h : passengers) {
            Vector2 p = this.position.copy();
            if (idx == 1) {
                p.x += 15f; // Hành khách thứ 2 đứng lệch 1 chút
            } else {
                p.x -= 15f;
            }
            h.setPosition(p);
            idx++;
        }
    }

    private void moveTo(Vector2 target, float speed, float dt) {
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist > 0) {
            float move = Math.min(speed * dt, dist);
            position.x += (dx/dist) * move;
            position.y += (dy/dist) * move;
        }
    }

    private Vector2 findFishingSpot() {
        if (getWorld() == null) return null;
        for (int i = 0; i < 50; i++) {
            float r = 200f + rand.nextFloat() * 300f;
            float angle = rand.nextFloat() * (float) Math.PI * 2;
            float tx = dockPosition.x + r * (float) Math.cos(angle);
            float ty = dockPosition.y + r * (float) Math.sin(angle);
            
            // Giới hạn trong bản đồ
            if (tx < 0 || tx >= getWorld().getWidth() || ty < 0 || ty >= getWorld().getHeight()) {
                continue;
            }

            // Tìm điểm nước sâu
            if (getWorld().getGameMap().isPositionInWater(tx, ty) && !isLineCrossingLand(dockPosition, new Vector2(tx, ty))) {
                return new Vector2(tx, ty);
            }
        }
        return null;
    }

    private boolean isLineCrossingLand(Vector2 start, Vector2 end) {
        if (getWorld() == null || getWorld().getGameMap() == null) return false;
        int steps = 100;
        // Skip the first 10% just in case the dock touches the shore
        for (int i = 10; i <= steps; i++) {
            float t = (float) i / steps;
            float px = start.x + t * (end.x - start.x);
            float py = start.y + t * (end.y - start.y);
            if (!getWorld().getGameMap().isPositionInWater(px, py)) {
                return true;
            }
        }
        return false;
    }
}

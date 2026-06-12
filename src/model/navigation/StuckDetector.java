package model.navigation;

import core.Vector2;
import model.living_beings.animal.Animal;
import model.world.World;

/**
 * Tách logic phát hiện kẹt (Stuck Detector) khỏi lớp Animal để giảm thiểu Coupling (Single Responsibility Principle).
 */
public class StuckDetector {

    private final Vector2 stuckCheckPos = new Vector2(0, 0);
    private boolean isInitialized = false;
    private float stuckTimer = 0f;
    private float escapeTimer = 0f;
    private Vector2 escapeDir = null;

    private static final float STUCK_THRESHOLD_TIME = 0.4f;
    private static final float STUCK_MOVE_THRESHOLD = 3.0f;
    private static final float ESCAPE_DURATION = 0.5f;

    private static final Vector2[] ESCAPE_DIRS = new Vector2[8];
    static {
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            ESCAPE_DIRS[i] = new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
        }
    }

    public void update(Animal animal, float deltaTime) {
        if (animal.getSpeed() <= 0 || escapeTimer > 0) {
            stuckTimer = 0;
            if (animal.getPosition() != null) {
                stuckCheckPos.set(animal.getPosition());
                isInitialized = true;
            }
            return;
        }

        if (!isInitialized && animal.getPosition() != null) {
            stuckCheckPos.set(animal.getPosition());
            isInitialized = true;
            return;
        }

        if (animal.getPosition() == null) return;

        float dist = animal.getPosition().distanceTo(stuckCheckPos);
        if (dist < STUCK_MOVE_THRESHOLD) {
            stuckTimer += deltaTime;
            if (stuckTimer >= STUCK_THRESHOLD_TIME) {
                escapeDir = findBestEscapeDirection(animal);
                escapeTimer = ESCAPE_DURATION;
                stuckTimer = 0;
            }
        } else {
            stuckTimer = 0;
            stuckCheckPos.set(animal.getPosition());
        }
    }

    public boolean isEscaping() {
        return escapeTimer > 0;
    }

    /** Reset trạng thái thoát — gọi khi con vật ẩn vào bụi hoặc bị bất động. */
    public void reset() {
        escapeTimer = 0f;
        escapeDir = null;
        stuckTimer = 0f;
    }

    public void doEmergencyEscape(Animal animal, float deltaTime) {
        escapeTimer -= deltaTime;
        if (escapeDir != null) {
            animal.setActionState("run");
            animal.setSpeed(animal.getBaseSpeed() * 1.5f);
            if (escapeDir.x > 0) {
                animal.setFacingRight(true);
            } else if (escapeDir.x < 0) {
                animal.setFacingRight(false);
            }
            animal.move(escapeDir, deltaTime);
        }
        
        if (escapeTimer <= 0) {
            escapeDir = null;
            if (animal.getPosition() != null) {
                stuckCheckPos.set(animal.getPosition());
            }
        }
    }

    private Vector2 findBestEscapeDirection(Animal animal) {
        World w = animal.getWorld();
        Vector2 bestDir = ESCAPE_DIRS[0]; 
        float maxClearDist = -1f;

        for (int i = 0; i < 8; i++) {
            Vector2 dir = ESCAPE_DIRS[i];
            float dx = dir.x;
            float dy = dir.y;

            float probeLen = 40f;
            float clearDist = probeLen;

            if (w != null && w.getSpatialGrid() != null && animal.getPosition() != null) {
                Vector2 probeEnd = new Vector2(
                    animal.getPosition().x + dx * probeLen,
                    animal.getPosition().y + dy * probeLen
                );
                
                if (!w.isValidPositionFor(animal, probeEnd)) {
                    clearDist = 0;
                }
                
                java.util.List<model.entity.Entity> nearby =
                    w.getSpatialGrid().getNeighbors(probeEnd, animal.getSize());
                    
                for (model.entity.Entity e : nearby) {
                    if (e != animal && e.isSolid() && e.isAlive()) {
                        float d = animal.getPosition().distanceTo(e.getPosition()) - animal.getSize() / 2 - e.getSize() / 2;
                        if (d < clearDist) clearDist = d;
                    }
                }
            }

            if (clearDist > maxClearDist) {
                maxClearDist = clearDist;
                bestDir = dir;
            }
        }
        return bestDir;
    }
}

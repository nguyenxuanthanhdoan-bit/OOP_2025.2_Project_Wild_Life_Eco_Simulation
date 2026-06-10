package model.strategies;

import model.living_beings.Animal;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.structures.Boat;
import model.world.World;
import core.Vector2;

public class BoardBoatStrategy implements IStrategy {
    private Boat targetBoat;
    private boolean boarded = false;
    private boolean finished = false;

    public BoardBoatStrategy(Boat boat) {
        this.targetBoat = boat;
    }

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Human)) return;
        Human animal = (Human) owner;
        
        if (targetBoat == null || targetBoat.getWorld() == null) {
            finished = true;
            return;
        }

        if (!boarded) {
            if (animal.getPosition().distanceTo(targetBoat.getPosition()) < 100f) {
                // Đã tới gần thuyền, lên thuyền
                if (targetBoat.canBoard()) {
                    targetBoat.board((Human) animal);
                    boarded = true;
                    animal.setSpeed(0);
                } else {
                    finished = true; // Thuyền đã chạy mất hoặc đầy
                }
            } else {
                Vector2 dir = targetBoat.getPosition().copy().subtract(animal.getPosition());
                if (dir.length() > 0) dir.normalize();
                animal.setSpeed(animal.getBaseSpeed());
                animal.move(dir, deltaTime);
            }
        } else {
            // Đã lên thuyền, chờ thuyền cập bến
            if (targetBoat.getState() == Boat.BoatState.DOCKED) {
                targetBoat.unboard((Human) animal);
                // Cập bến, đẩy lên bờ hướng về phía làng (homeCenter)
                Vector2 dir = animal.getHomeCenter().copy().subtract(animal.getPosition());
                if (dir.length() > 0) dir.normalize();
                animal.getPosition().add(dir.scale(100f)); // đẩy về bờ 100px

                finished = true;
            }
        }
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return finished;
    }

    @Override
    public int getPriority() { return 60; }

    @Override
    public String getName() { return "BoardBoat"; }
}

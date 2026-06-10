package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Human;
import model.navigation.PathNavigator;
import model.structures.GardenBed;
import model.world.World;

/**
 * AI Strategy cho Human: Đi đến chậu cây chín và thu hoạch.
 */
public class HarvestStrategy implements IStrategy {

    private GardenBed targetBed;
    private final PathNavigator navigator = new PathNavigator();
    private float harvestTimer = 0f;
    private static final float HARVEST_DURATION = 3.0f; // 3 giây để thu hoạch

    public HarvestStrategy(GardenBed targetBed) {
        this.targetBed = targetBed;
        targetBed.setBeingHarvested(true);
    }

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Human) || targetBed == null || world == null) {
            return;
        }
        
        Human human = (Human) owner;

        // Nếu chậu không còn chín hoặc đã bị người khác thu hoạch (thực ra đã set flag)
        if (!targetBed.isMature()) {
            targetBed.setBeingHarvested(false); // nhả lock
            return;
        }

        float dist = owner.getPosition().distanceTo(targetBed.getPosition());
        
        // Khoảng cách tới sát chậu cây (ví dụ <= 60 pixels)
        if (dist <= 60.0f) {
            // Đã đến nơi, bắt đầu quá trình thu hoạch
            human.setActionState("idle"); 
            human.setSpeed(0);

            harvestTimer += deltaTime;
            if (harvestTimer >= HARVEST_DURATION) {
                // Hoàn thành thu hoạch
                targetBed.harvest();
            }
        } else {
            // Đang đi tới chậu
            human.setActionState("walk");
            human.setSpeed(human.getBaseSpeed() * 0.5f);
            boolean reached = navigator.moveTo((model.living_beings.Animal) owner, world, targetBed.getPosition(), deltaTime, 18.0f, 1.5f);
            
            if (navigator.isBlocked()) {
                // Không tìm được đường đến
                targetBed.setBeingHarvested(false);
                navigator.clear();
            }
        }
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return targetBed == null || !targetBed.isMature();
    }

    @Override
    public int getPriority() {
        return 70; // Ưu tiên cao hơn Wander (50)
    }

    @Override
    public String getName() {
        return "Harvest";
    }

    @Override
    public Vector2 getTarget() {
        return targetBed != null ? targetBed.getPosition() : null;
    }

    @Override
    public java.util.List<Vector2> getPath() {
        return navigator.getPath();
    }
}

package model.strategies;

import model.living_beings.LivingBeing;
import model.world.World;

public interface IStrategy {
    // Thực thi hành động (di chuyển, đứng yên...)
    void execute(LivingBeing owner, World world, float deltaTime);

    // Kiểm tra xem có nên ngắt chiến thuật này không
    boolean shouldInterrupt(LivingBeing owner, World world);

    int getPriority();
    String getName();

    default core.Vector2 getTarget() { return null; }
    default java.util.List<core.Vector2> getPath() { return java.util.Collections.emptyList(); }
}
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

    // ── Lifecycle hooks ───────────────────────────────────────────
    /** Được gọi khi strategy này được kích hoạt. Dùng để reserve resource. */
    default void onEnter(LivingBeing owner, World world) {}

    /** Được gọi khi strategy bị thay thế. Dùng để release resource. */
    default void onExit(LivingBeing owner, World world)  {}

    /**
     * Trả về true nếu task đang ở giai đoạn cam kết:
     * không nên bị ngắt bởi công việc ưu tiên thấp hơn.
     * (VD: HarvestStrategy đang đứng thu hoạch)
     */
    default boolean isCommittedTask() { return false; }

    /**
     * Trả về true nếu task đang ở giai đoạn KHÔNG THỂ ngắt về mặt vật lý.
     * (VD: Fisherman đang trên thuyền ngoài biển)
     */
    default boolean isInNonInterruptiblePhase() { return false; }
}
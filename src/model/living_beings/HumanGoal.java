package model.living_beings;

/**
 * Hệ thống Goal-based AI cho Human.
 *
 * Thứ tự ưu tiên:
 *   FLEE > GO_HOME > SLEEP > WANDER
 *
 * Mỗi frame Human gọi updateGoal() rồi executeGoal() thông qua
 * StrategySelector — không cần if-else dài trong update().
 * Về sau có thể mở rộng thêm: GATHER_RESOURCE, BUILD_HOUSE, TRADE, FARM...
 */
public enum HumanGoal {

    /** Ban ngày: lang thang trong homeArea, khám phá bản đồ. */
    WANDER,

    /** Ban đêm: di chuyển về ngôi nhà gần nhất để ngủ. */
    GO_HOME,

    /** Đang ở trong nhà, nghỉ ngơi cho đến buổi sáng. */
    SLEEP,

    /** Phát hiện mối đe dọa (Wolf / Tiger / Elephant) — ưu tiên cao nhất. */
    FLEE
}

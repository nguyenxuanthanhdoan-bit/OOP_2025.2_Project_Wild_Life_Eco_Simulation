package model.living_beings;

/**
 * Nghề nghiệp của Human. Chứa toàn bộ capability của role,
 * không để logic phân công rải rác trong các class khác.
 *
 * Thêm role mới (FARMER, BUILDER...) chỉ cần khai báo ở đây,
 * không sửa StrategySelector hay Human.
 */
public enum HumanRole {
    //                harvest  fish   hunt   house  reproduce  canBeHunted
    VILLAGER  (true,  false, false, true,  true,  true),
    HUNTER    (false, false, true,  true,  false, false),
    FISHERMAN (false, true,  false, true,  true,  true);

    private final boolean canHarvest;
    private final boolean canFish;
    private final boolean canHunt;
    private final boolean canUseHouse;
    private final boolean canReproduce;
    private final boolean canBeHunted;

    HumanRole(boolean canHarvest, boolean canFish, boolean canHunt,
              boolean canUseHouse, boolean canReproduce, boolean canBeHunted) {
        this.canHarvest    = canHarvest;
        this.canFish       = canFish;
        this.canHunt       = canHunt;
        this.canUseHouse   = canUseHouse;
        this.canReproduce  = canReproduce;
        this.canBeHunted   = canBeHunted;
    }

    public boolean canHarvest() { return canHarvest; }
    public boolean canFish() { return canFish; }
    public boolean canHunt() { return canHunt; }
    public boolean canUseHouse() { return canUseHouse; }
    public boolean canReproduce() { return canReproduce; }
    public boolean canBeHunted() { return canBeHunted; }
}

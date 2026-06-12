package model.living_beings;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.items.Carcass;
import model.items.FoodSource;
import model.entity.Entity;
import model.plants.Fruit;
import model.plants.Mushroom;
import model.structures.FoodStorage;
import model.structures.House;
import model.strategies.PassiveStrategy;

import java.util.Random;

public class Human extends Animal {
    public enum Variant {
        MALE("human_male"),
        FEMALE("human_female");

        private final String spriteKey;

        Variant(String spriteKey) {
            this.spriteKey = spriteKey;
        }
    }

    private static final float SIZE = 32.0f;
    // Profile dùng chung cho Villager và Fisherman (cả hai đều là dân thường)
    protected static final AnimalProfile CIVILIAN_PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_HERBIVORE)
            .canBeScared(true)
            .canHide(true)
            .canEatPlants(true)
            .ediblePlants(Fruit.class, Mushroom.class)
            .canEnterGardens(true)
            .build();
    protected static final AnimalProfile HUNTER_PROFILE = AnimalProfile.builder()
            .entityLevel(LEVEL_APEX_ANIMAL)
            .canHunt(true)
            .canEatMeat(true)
            .attackDamagePerSecond(90.0f)
            .maxPreySizeMultiplier(2.0f)
            .canEnterGardens(true)
            .build();

    private final String spriteKey;
    private final Variant variant;
    private final HumanRole role;
    private final Vector2 homeCenter;
    private final float homeRadius;
    private final float carryCapacity;
    private float carriedFood;
    private House hiddenInHouse = null;
    private model.world.Settlement homeSettlement;

    public Human(Vector2 position) {
        this(position, Variant.MALE);
    }

    public Human(Vector2 position, Variant variant) {
        this(position, variant, position, GameConfig.getInstance().VILLAGE_STRUCTURE_CLUSTER_RADIUS);
    }

    public Human(Vector2 position, Variant variant, Vector2 homeCenter, float homeRadius) {
        this(position, variant, HumanRole.VILLAGER, homeCenter, homeRadius);
    }

    public Human(Vector2 position, Variant variant, HumanRole role, Vector2 homeCenter, float homeRadius) {
        this(position, SIZE, GameConfig.getInstance().HUMAN_BASE_SPEED, "Dân làng",
                variant == null ? Variant.MALE.spriteKey : variant.spriteKey,
                role, homeCenter, homeRadius,
                GameConfig.getInstance().HUMAN_CARRY_CAPACITY);
    }

    protected Human(Vector2 position, float size, float baseSpeed, String speciesName, String spriteKey,
                    HumanRole role, Vector2 homeCenter, float homeRadius, float carryCapacity) {
        super(position, size, baseSpeed, DietType.OMNIVORE);
        HumanRole normalizedRole = role == null ? HumanRole.VILLAGER : role;
        this.speciesName = speciesName;
        this.spriteKey = spriteKey;
        this.variant = resolveVariant(spriteKey, normalizedRole);
        this.role = normalizedRole;
        this.homeCenter = homeCenter == null ? position.copy() : homeCenter.copy();
        this.homeRadius = Math.max(80.0f, homeRadius);
        this.carryCapacity = Math.max(0.0f, carryCapacity);
        this.carriedFood = 0.0f;
        this.maxHealth = 120.0f;
        this.health = this.maxHealth;
        this.maxHunger = 120.0f;
        this.hunger = this.maxHunger;
        this.hungerDecayRate = 0.08f;
        this.maxThirst = 120.0f;
        this.thirst = this.maxThirst;
        this.thirstDecayRate = 0.12f;
        this.maxAge = 10000.0f;
        this.visionRange = 260.0f;
        this.adult = true;
        this.profile = this.role.canHunt() ? HUNTER_PROFILE : CIVILIAN_PROFILE;
        setStrategy(new PassiveStrategy());
    }

    private static Variant resolveVariant(String spriteKey, HumanRole role) {
        if (role.canHunt()) return null;
        return Variant.FEMALE.spriteKey.equals(spriteKey) ? Variant.FEMALE : Variant.MALE;
    }

    @Override
    public boolean canReproduce() {
        return role.canReproduce() && super.canReproduce();
    }

    @Override
    public boolean canMateWith(Animal other) {
        if (!(other instanceof Human) || !super.canMateWith(other)) return false;
        Human human = (Human) other;
        return role.canReproduce()
                && human.role.canReproduce()
                && homeSettlement != null
                && homeSettlement == human.homeSettlement
                && variant != null
                && human.variant != null
                && variant != human.variant;
    }

    @Override
    public String getSpriteKey() {
        if (role != null && role.canHunt()) return "human_hunter";
        return spriteKey;
    }

    @Override
    public boolean isSpriteFacingRightByDefault() {
        return true;
    }

    public HumanRole getRole() {
        return role;
    }

    public boolean isVillager() {
        return role == HumanRole.VILLAGER;
    }

    public boolean isHunter() {
        return role == HumanRole.HUNTER;
    }

    // =========================================================
    // CAPABILITY METHODS — delegate to role
    // =========================================================

    public boolean canHarvest()        { return role.canHarvest(); }
    public boolean canFish()           { return role.canFish(); }
    public boolean canHuntRole()       { return role.canHunt(); }
    public boolean canHuntForVillage() { return role.canHunt() && shouldHuntForVillage(); }
    public boolean canUseHouse()       { return role.canUseHouse(); }
    public boolean canReproduceRole()  { return role.canReproduce(); }

    public Variant getVariant() {
        return variant;
    }

    public boolean isMale() {
        return variant == Variant.MALE;
    }

    public boolean isFemale() {
        return variant == Variant.FEMALE;
    }

    public Vector2 getHomeCenter() {
        return homeCenter.copy();
    }

    public float getHomeRadius() {
        return homeRadius;
    }

    public boolean isInHomeArea() {
        return isInHomeArea(getPosition());
    }

    public boolean isInHomeArea(Vector2 pos) {
        return pos != null && pos.distanceTo(homeCenter) <= homeRadius;
    }

    public float getCarriedFood() {
        return carriedFood;
    }

    public float getCarryCapacity() {
        return carryCapacity;
    }

    public model.world.Settlement getHomeSettlement() {
        return homeSettlement;
    }

    public void setHomeSettlement(model.world.Settlement settlement) {
        this.homeSettlement = settlement;
    }

    public boolean hasCarriedFood() {
        return carriedFood > 0.01f;
    }

    public boolean isCarryingFoodAtLeast(float ratio) {
        if (carryCapacity <= 0) return false;
        float clampedRatio = Math.max(0.0f, Math.min(1.0f, ratio));
        return carriedFood >= carryCapacity * clampedRatio;
    }

    public float addCarriedFood(float amount) {
        if (amount <= 0 || carryCapacity <= 0) return 0.0f;
        float accepted = Math.min(amount, carryCapacity - carriedFood);
        carriedFood += accepted;
        return accepted;
    }

    public float consumeCarriedFood(float amount) {
        if (amount <= 0 || carriedFood <= 0) return 0.0f;
        float consumed = Math.min(amount, carriedFood);
        carriedFood -= consumed;
        return consumed;
    }

    public float depositFood(FoodStorage storage) {
        if (storage == null || carriedFood <= 0 || !isNearStructure(storage, 20.0f)) return 0.0f;
        float deposited = storage.addFood(carriedFood);
        carriedFood -= deposited;
        return deposited;
    }

    public float eatFromStorage(FoodStorage storage, float amount) {
        if (storage == null || amount <= 0 || !storage.hasFood() || !isNearStructure(storage, 18.0f)) {
            return 0.0f;
        }
        float consumed = storage.consumeFood(amount);
        this.hunger = Math.min(this.maxHunger, this.hunger + consumed);
        return consumed;
    }

    public void eatCarriedFood(float amount) {
        float consumed = consumeCarriedFood(amount);
        if (consumed > 0) {
            this.hunger = Math.min(this.maxHunger, this.hunger + consumed);
        }
    }

    public boolean shouldHuntForVillage() {
        if (!role.canHunt() || carriedFood > 0.01f) return false;
        if (hunger < maxHunger * 0.95) return true;
        FoodStorage storage = findHomeFoodStorage();
        return storage != null && storage.getStoredFood() < storage.getCapacity() * 0.35f;
    }

    private FoodStorage findHomeFoodStorage() {
        if (worldRef == null) return null;
        if (homeSettlement != null) {
            return homeSettlement.findNearestFoodStorage(position, false);
        }
        Iterable<Entity> candidates = worldRef.getSpatialGrid() != null
                ? worldRef.getSpatialGrid().getNeighbors(homeCenter, homeRadius)
                : worldRef.getEntities();
        FoodStorage best = null;
        float bestDist = Float.MAX_VALUE;
        for (Entity entity : candidates) {
            if (!(entity instanceof FoodStorage) || !entity.isAlive()) continue;
            if (!isInHomeArea(entity.getPosition())) continue;
            float dist = homeCenter.distanceTo(entity.getPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = (FoodStorage) entity;
            }
        }
        return best;
    }

    /**
     * Vào nhà khi ở gần (dùng bởi ScaredStrategy khi bị đe dọa).
     * Yêu cầu phải đứng gần nhà đủ để vào.
     */
    public boolean enterHouse(House house) {
        if (house == hiddenInHouse) return true;
        if (house == null || !house.hasSpace()
                || !isNearStructure(house, core.GameConfig.getInstance().HOUSE_ENTER_DISTANCE)) {
            return false;
        }
        if (!house.enter(this)) return false;
        applyHiddenInHouse(house);
        return true;
    }

    /**
     * Đi ngủ ban đêm — biến mất ngay khi chạm vào nhà.
     * Không cần điều kiện khoảng cách nghiêm ngặt.
     * Chỉ cần đang thực sự "chạm" (isTouchingHouse == true).
     */
    public boolean goSleep(House house) {
        if (house == null || !house.isAlive()) return false;
        if (!house.hasSpace() && house != hiddenInHouse) return false;
        if (!isTouchingHouse(house)) return false;
        if (!house.enter(this)) return false;
        applyHiddenInHouse(house);
        return true;
    }

    /**
     * Kiểm tra Human có đang chạm vào nhà không
     * (tâm human cách tâm nhà ≤ human.radius + house.radius).
     */
    public boolean isTouchingHouse(House house) {
        if (house == null) return false;
        float humanRadius = getCollider() != null
                ? getCollider().getRadius() : getSize() * 0.4f;
        float houseRadius = house.getCollider() != null
                ? house.getCollider().getRadius() : house.getSize() * 0.4f;
        // Tạo một vùng bao quanh nhà, chạm vào vùng này sẽ vào nhà luôn
        float touchDist = humanRadius + houseRadius + core.GameConfig.getInstance().HOUSE_ENTER_DISTANCE;
        return this.getPosition().distanceTo(house.getPosition()) <= touchDist;
    }

    public boolean isTouchingFishingHut() {
        if (worldRef == null || worldRef.getCoastalManager() == null) return false;
        float humanRadius = getCollider() != null
                ? getCollider().getRadius() : getSize() * 0.4f;
        
        for (model.structures.FishingHut hut : worldRef.getCoastalManager().getFishingHuts()) {
            if (!hut.isAlive()) continue;
            float hutRadius = hut.getCollider() != null
                    ? hut.getCollider().getRadius() : hut.getSize() * 0.4f;
            float touchDist = humanRadius + hutRadius + 15.0f; // tolerance for touching
            if (this.getPosition().distanceTo(hut.getPosition()) <= touchDist) {
                return true;
            }
        }
        return false;
    }

    private void applyHiddenInHouse(House house) {
        this.hidden = true;
        this.hiddenInHouse = house;
        this.isMoving = false;
        this.setSpeed(0);
        this.setActionState("idle");
        this.currentVelocity.set(0, 0);
        this.setPosition(house.getPosition());
    }

    public void exitHouse() {
        if (hiddenInHouse != null) {
            hiddenInHouse.exit(this);
            hiddenInHouse = null;
        }
        this.hidden = false;
        this.setSpeed(this.baseSpeed);
    }

    public boolean tryExitHouseSafely() {
        if (hiddenInHouse == null) return true;
        Vector2 exitPosition = findWakeupPosition(hiddenInHouse);
        if (exitPosition == null) return false;
        exitHouse();
        setPosition(exitPosition);
        return true;
    }

    public House getHiddenInHouse() {
        return hiddenInHouse;
    }

    private boolean isNearStructure(model.entity.Structure structure, float padding) {
        if (structure == null) return false;
        float range = this.getSize() / 2 + structure.getSize() / 2 + padding;
        return this.getPosition().distanceTo(structure.getPosition()) <= range;
    }

    @Override
    public Animal reproduce() {
        Variant childVariant = Math.random() < 0.5 ? Variant.MALE : Variant.FEMALE;
        Human child = new Human(getPosition().copy(), childVariant, homeCenter, homeRadius);
        child.setHomeSettlement(homeSettlement);
        child.setAge(0);
        child.size = SIZE * 0.55f;
        child.setAdult(false);
        return child;
    }

    @Override
    protected model.items.Carcass createCarcass() {
        return new model.items.Carcass(
                position.copy(), 24.0f, 80.0f, 120.0f, 80.0f, speciesName, true);
    }

    /**
     * Vòng cập nhật chính của Human.
     *
     * Logic thoát nhà (wake up):
     *   - Trời sáng (!isNight) VÀ không có mối đe dọa → thoát nhà và spawn gần nhà.
     *   - Nếu vẫn là ban đêm: không thoát (GoHomeStrategy giữ hidden = true).
     *   - Nếu có mối đe dọa ban ngày: ScaredStrategy sẽ xử lý thoát nhà sau.
     */
    @Override
    public void update(float deltaTime) {
        if (hiddenInHouse != null) {
            boolean isNight = worldRef != null &&
                    (worldRef.getTimeOfDay() >= 18.0f || worldRef.getTimeOfDay() <= 5.0f);
            boolean threatened = hasDangerousThreats();

            // Chỉ thoát nhà khi trời sáng VÀ an toàn
            if (!isNight && !threatened) {
                tryExitHouseSafely();
            }
        }
        super.update(deltaTime);
        updateAnimation();
    }

    /**
     * Tìm vị trí xuất hiện sau khi ngủ dậy, ngắm quanh nhà trong bán kính nhỏ.
     * Đảm bảo vị trí là mặt đất hợp lệ (không nước, không thoát bản đồ).
     */
    private Vector2 findWakeupPosition(House house) {
        if (house == null) return null;
        Vector2 housePos = house.getPosition();
        model.world.World currentWorld = worldRef != null ? worldRef : getWorld();
        if (currentWorld == null) return null;

        Random rng = new Random();
        float spawnRadius = house.getSize() + this.size + 20.0f;

        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist  = spawnRadius * (0.6 + rng.nextDouble() * 0.6);
            float cx = (float) (housePos.x + Math.cos(angle) * dist);
            float cy = (float) (housePos.y + Math.sin(angle) * dist);
            Vector2 candidate = new Vector2(cx, cy);
            if (currentWorld.isValidPositionFor(this, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Kiểm tra Human có đang ngủ trong nhà hay không.
     */
    public boolean isSleeping() {
        return hidden && hiddenInHouse != null;
    }

    @Override
    public boolean canHunt(Animal prey) {
        if (prey instanceof Human) return false;
        return super.canHunt(prey);
    }

    /**
     * Con người có thể bị săn bởi động vật dựa theo role:
     * Villager và Fisherman có thể bị săn; Hunter không thể bị săn.
     */
    @Override
    public boolean canBeHuntedBy(Animal predator) {
        return role.canBeHunted();
    }

    @Override
    public boolean canEatFoodSource(FoodSource food) {
        if (food instanceof Carcass && ((Carcass) food).isHumanSource()) return false;
        return super.canEatFoodSource(food);
    }

    @Override
    public void die(String reason) {
        exitHouse();
        super.die(reason);
    }

    private void updateAnimation() {
        String animationState = getAnimationState();
        imageVariant = ("run".equals(animationState) || "walk".equals(animationState)
                || "attack".equals(animationState) || "eat".equals(animationState)
                || "drink".equals(animationState))
                ? animationState + ".png"
                : "idle.png";
    }

    @Override
    public void render(DisplayMode mode) {
    }
}

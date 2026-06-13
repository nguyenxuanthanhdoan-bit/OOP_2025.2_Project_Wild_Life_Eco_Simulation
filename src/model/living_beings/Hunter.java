package model.living_beings;

import core.GameConfig;
import core.Vector2;

public class Hunter extends Human {
    private static final float SIZE = 34.0f;
    private int ammo;
    private final int maxAmmo;
    private float fireCooldownTimer = 0.0f;
    private float reloadTimer = 0.0f;

    public Hunter(Vector2 position) {
        this(position, position, GameConfig.getInstance().VILLAGE_STRUCTURE_CLUSTER_RADIUS);
    }

    public Hunter(Vector2 position, Vector2 homeCenter, float homeRadius) {
        super(position, SIZE, GameConfig.getInstance().HUNTER_BASE_SPEED, "Thợ săn", "human_hunter",
                HumanRole.HUNTER, homeCenter, homeRadius, GameConfig.getInstance().HUNTER_CARRY_CAPACITY);
        this.setMaxHealth(160.0f);
        this.setHealth(160.0f);
        this.setVisionRange(306.72f);
        this.setHungerDecayRate(0.11f);
        this.setThirstDecayRate(0.14f);
        this.maxAmmo = GameConfig.getInstance().HUNTER_MAX_AMMO;
        this.ammo = this.maxAmmo;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getMaxAmmo() {
        return maxAmmo;
    }

    public boolean hasAmmo() {
        return ammo > 0;
    }

    public boolean needsAmmoReload() {
        return ammo <= 0;
    }

    public boolean canShoot() {
        return hasAmmo() && fireCooldownTimer <= 0.0f;
    }

    public void tickFireCooldown(float deltaTime) {
        if (fireCooldownTimer > 0.0f) {
            fireCooldownTimer = Math.max(0.0f, fireCooldownTimer - deltaTime);
        }
    }

    public void resetFireCooldown() {
        fireCooldownTimer = GameConfig.getInstance().HUNTER_FIRE_COOLDOWN_SECONDS;
    }

    public void consumeAmmo() {
        if (ammo > 0) {
            ammo--;
        }
    }

    public boolean reloadAtHome(float deltaTime) {
        reloadTimer += deltaTime;
        if (reloadTimer >= GameConfig.getInstance().HUNTER_RELOAD_SECONDS) {
            ammo = maxAmmo;
            reloadTimer = 0.0f;
            return true;
        }
        return false;
    }

    public void cancelReload() {
        reloadTimer = 0.0f;
    }

    @Override
    public Animal reproduce() {
        return new Hunter(getPosition().copy());
    }
}

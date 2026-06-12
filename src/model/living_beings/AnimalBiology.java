package model.living_beings;

public class AnimalBiology {
    private double health;
    private double maxHealth;
    private double hunger;
    private double maxHunger;
    private double hungerDecayRate;
    private double thirst;
    private double maxThirst;
    private double thirstDecayRate;
    private double age;
    private double maxAge;
    private boolean adult;
    private float damageBlinkTimer = 0.0f;

    public void takeDamage(double amount) {
        this.health = Math.max(0, this.health - amount);
        this.damageBlinkTimer = 0.15f;
    }

    public void heal(double amount) {
        this.health = Math.min(this.maxHealth, this.health + amount);
    }

    public void updateNeeds(float deltaTime, float decayMultiplier) {
        this.hunger = Math.max(0, this.hunger - (this.hungerDecayRate * decayMultiplier * deltaTime * 0.25f));
        this.thirst = Math.max(0, this.thirst - (this.thirstDecayRate * decayMultiplier * deltaTime * 0.25f));
    }

    public void growOlder(double deltaTime) {
        this.age += deltaTime;
    }

    public void decreaseDamageBlinkTimer(float deltaTime) {
        if (damageBlinkTimer > 0) {
            damageBlinkTimer -= deltaTime;
        }
    }

    // Getters & Setters
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = Math.max(0, Math.min(maxHealth, health)); }
    
    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    public double getHunger() { return hunger; }
    public void setHunger(double hunger) { this.hunger = Math.max(0, Math.min(maxHunger, hunger)); }
    
    public double getMaxHunger() { return maxHunger; }
    public void setMaxHunger(double maxHunger) { this.maxHunger = maxHunger; }

    public double getHungerDecayRate() { return hungerDecayRate; }
    public void setHungerDecayRate(double hungerDecayRate) { this.hungerDecayRate = hungerDecayRate; }

    public double getThirst() { return thirst; }
    public void setThirst(double thirst) { this.thirst = Math.max(0, Math.min(maxThirst, thirst)); }
    
    public double getMaxThirst() { return maxThirst; }
    public void setMaxThirst(double maxThirst) { this.maxThirst = maxThirst; }

    public double getThirstDecayRate() { return thirstDecayRate; }
    public void setThirstDecayRate(double thirstDecayRate) { this.thirstDecayRate = thirstDecayRate; }

    public double getAge() { return age; }
    public void setAge(double age) { this.age = age; }

    public double getMaxAge() { return maxAge; }
    public void setMaxAge(double maxAge) { this.maxAge = maxAge; }

    public boolean isAdult() { return adult; }
    public void setAdult(boolean adult) { this.adult = adult; }

    public float getDamageBlinkTimer() { return damageBlinkTimer; }
    public void setDamageBlinkTimer(float damageBlinkTimer) { this.damageBlinkTimer = damageBlinkTimer; }
}

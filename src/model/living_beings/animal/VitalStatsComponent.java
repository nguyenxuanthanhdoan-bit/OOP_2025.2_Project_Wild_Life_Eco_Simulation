package model.living_beings.animal;

import model.plants.Plant;
import model.items.FoodSource;
import model.items.Carcass;
import model.world.PopulationManager;
import core.GameConfig;

public class VitalStatsComponent {
    private final Animal owner;

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
    private boolean adult = false;

    private float damageBlinkTimer = 0f;

    public VitalStatsComponent(Animal owner) {
        this.owner = owner;
    }

    public void update(float deltaTime) {
        if (!owner.isAlive()) return;

        // Xử lý decay hunger/thirst
        double baseThirstDecay = 1.0;
        if (owner.getProfile() != null && owner.getProfile().isDesertAdapted()) {
            baseThirstDecay = 0.5;
        }
        
        float currentDelta = deltaTime;
        if (owner.isHidden()) currentDelta *= 0.2f;

        // Mùa đông tiêu hao nhanh hơn
        if (owner.getWorldRef() != null && owner.getWorldRef().getCurrentSeason() == model.world.World.Season.WINTER) {
            float winterPenalty = 1.0f + owner.getWorldRef().getWinterProgress() * 1.5f; 
            if (owner.isHidden()) {
                winterPenalty = 1.0f; // Trốn trong nhà/bụi thì đỡ lạnh
            }
            currentDelta *= winterPenalty;
        }

        hunger -= hungerDecayRate * currentDelta;
        thirst -= thirstDecayRate * baseThirstDecay * currentDelta;

        if (hunger <= 0 || thirst <= 0) {
            owner.setSpeed(owner.getBaseSpeed() * 0.3f); 
            takeDamage(5.0 * deltaTime); 
        }

        if (damageBlinkTimer > 0) {
            damageBlinkTimer -= deltaTime;
        }
    }

    public void takeDamage(double amount) {
        if (!owner.isAlive()) return;
        health -= amount;
        damageBlinkTimer = 0.2f;
        
        if (health <= 0) {
            health = 0;
            owner.die("Starved");
        }
    }

    public void heal(double amount) {
        if (!owner.isAlive()) return;
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }

    public void eat(Plant food) {
        if (food != null && food.isAlive() && owner.canEatPlant(food)) {
            hunger += food.getNutritionValue();
            if (hunger > maxHunger) hunger = maxHunger;
            food.setAlive(false);
        }
    }

    public void eatMeat(FoodSource food) {
        if (food != null && food.isAlive() && owner.getProfile() != null && owner.getProfile().canEatMeat()) {
            hunger += food.consume((float) owner.getMaxHunger());
            if (hunger > maxHunger) hunger = maxHunger;
            food.setAlive(false);
        }
    }

    public void eatCarcass(Carcass carcass, float deltaTime) {
        if (carcass == null || !carcass.isAlive()) return;
        float eatAmount = 20.0f * deltaTime; 
        float actualEaten = carcass.consume(eatAmount);
        hunger += actualEaten;
        if (hunger > maxHunger) hunger = maxHunger;
    }

    public void drink() {
        thirst += 30.0;
        if (thirst > maxThirst) thirst = maxThirst;
    }

    public void growOlder(double deltaTime) {
        age += deltaTime;
        if (!adult && age > maxAge * 0.2) {
            adult = true;
            
            owner.setMaxHealth(maxHealth * 2f);
            this.health = maxHealth;
        }
        if (age > maxAge) {
            owner.die("Old age");
        }
    }

    // Getters / Setters
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }
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
}

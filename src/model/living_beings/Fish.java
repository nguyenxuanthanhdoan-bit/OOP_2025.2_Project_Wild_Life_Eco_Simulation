package model.living_beings;

import core.Vector2;
import model.items.Carcass;

public abstract class Fish extends Animal {
    protected HabitatRule habitatRule;

    public Fish(
            Vector2 position, float size, float baseSpeed,
            String speciesName,
            double maxHealth, double maxHunger, double hungerDecayRate,
            double maxThirst, double thirstDecayRate,
            double maxAge, double visionRange, DietType dietType,
            HabitatRule habitatRule
    ) {
        super(position, size, baseSpeed, speciesName, maxHealth, maxHunger, hungerDecayRate,
              maxThirst, thirstDecayRate, maxAge, visionRange, dietType);
        this.habitatRule = habitatRule;
    }

    public Fish(Vector2 position, float size, float baseSpeed, DietType dietType, HabitatRule habitatRule) {
        super(position, size, baseSpeed, dietType);
        this.habitatRule = habitatRule;
    }

    public HabitatRule getHabitatRule() {
        return habitatRule;
    }

    @Override
    protected Carcass createCarcass() {
        // Cá chết không sinh ra thịt, chỉ biến mất
        return null;
    }
    
    @Override
    public boolean isNearWater() {
        return true; // Cá luôn ở trong nước nên không bao giờ khát theo cách bình thường
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        this.thirst = this.maxThirst; // Ép khát luôn đầy vì ở trong nước
        
        // Cập nhật imageVariant cơ bản
        this.imageVariant = this.speciesName + ".png"; 
    }

    @Override
    public void render(core.DisplayMode mode) {}
}

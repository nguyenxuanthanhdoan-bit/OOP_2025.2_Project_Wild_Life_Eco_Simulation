package model.living_beings.animal;

public class ReproductionComponent {
    private final Animal owner;
    private float reproductionCooldown = 0.0f;

    public ReproductionComponent(Animal owner) {
        this.owner = owner;
    }

    public void update(float deltaTime) {
        if (reproductionCooldown > 0) {
            reproductionCooldown = Math.max(0.0f, reproductionCooldown - deltaTime);
        }
    }

    public boolean canReproduce() {
        if (!owner.isAlive() || !owner.isAdult()) return false;
        if (reproductionCooldown > 0.0f) return false;
        if (owner.getAge() < owner.getMaxAge() * 0.2 || owner.getAge() > owner.getMaxAge() * 0.8) return false;
        if (owner.getHunger() < owner.getMaxHunger() * 0.7 || owner.getThirst() < owner.getMaxThirst() * 0.7) return false;

        // Giới hạn dân số toàn map
        model.world.World worldRef = owner.getWorldRef();
        if (worldRef != null && worldRef.getAnimalCount() >= core.GameConfig.getInstance().MAX_ANIMAL_POPULATION) return false;

        return true;
    }

    public void startReproductionCooldown() {
        reproductionCooldown = core.GameConfig.getInstance().REPRODUCTION_COOLDOWN_SECONDS;
    }

    public boolean canMateWith(Animal other) {
        return other != null
                && other != owner
                && other.isAlive()
                && other.canReproduce()
                && other.getSpeciesName().equals(owner.getSpeciesName());
    }

    public float getReproductionCooldown() {
        return reproductionCooldown;
    }

    public void setReproductionCooldown(float reproductionCooldown) {
        this.reproductionCooldown = reproductionCooldown;
    }
}

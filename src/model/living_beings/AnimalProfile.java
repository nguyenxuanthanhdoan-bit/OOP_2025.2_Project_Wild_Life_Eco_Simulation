package model.living_beings;

import model.entity.Entity;
import model.plants.Plant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AnimalProfile {
    private final int entityLevel;
    private final boolean canHunt;
    private final boolean canEatMeat;
    private final boolean canEatPlants;
    private final boolean canFlock;
    private final boolean canHide;
    private final boolean canBeScared;
    private final boolean eatOwnSpecies;
    private final float attackDamagePerSecond;
    private final float maxPreySizeMultiplier;
    private final FlockingMode flockingMode;
    private final Set<Class<? extends Plant>> ediblePlantTypes;

    private AnimalProfile(Builder builder) {
        this.entityLevel = builder.entityLevel;
        this.canHunt = builder.canHunt;
        this.canEatMeat = builder.canEatMeat;
        this.canEatPlants = builder.canEatPlants;
        this.canFlock = builder.canFlock;
        this.canHide = builder.canHide;
        this.canBeScared = builder.canBeScared;
        this.eatOwnSpecies = builder.eatOwnSpecies;
        this.attackDamagePerSecond = builder.attackDamagePerSecond;
        this.maxPreySizeMultiplier = builder.maxPreySizeMultiplier;
        this.flockingMode = builder.flockingMode;
        this.ediblePlantTypes = Collections.unmodifiableSet(new HashSet<>(builder.ediblePlantTypes));
    }

    public static AnimalProfile defaultFor(DietType dietType) {
        Builder builder = builder();
        if (dietType == DietType.CARNIVORE) {
            return builder
                    .entityLevel(Entity.LEVEL_CARNIVORE)
                    .canHunt(true)
                    .canEatMeat(true)
                    .attackDamagePerSecond(70.0f)
                    .maxPreySizeMultiplier(1.5f)
                    .build();
        }
        if (dietType == DietType.HERBIVORE) {
            return builder
                    .entityLevel(Entity.LEVEL_HERBIVORE)
                    .canEatPlants(true)
                    .canBeScared(true)
                    .canHide(true)
                    .build();
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean canEatPlant(Plant plant) {
        if (!canEatPlants || plant == null) return false;
        if (ediblePlantTypes.isEmpty()) return true;
        for (Class<? extends Plant> plantType : ediblePlantTypes) {
            if (plantType.isInstance(plant)) return true;
        }
        return false;
    }

    public int getEntityLevel() { return entityLevel; }
    public boolean canHunt() { return canHunt; }
    public boolean canEatMeat() { return canEatMeat; }
    public boolean canEatPlants() { return canEatPlants; }
    public boolean canFlock() { return canFlock; }
    public boolean canHide() { return canHide; }
    public boolean canBeScared() { return canBeScared; }
    public boolean canEatOwnSpecies() { return eatOwnSpecies; }
    public float getAttackDamagePerSecond() { return attackDamagePerSecond; }
    public float getMaxPreySizeMultiplier() { return maxPreySizeMultiplier; }
    public FlockingMode getFlockingMode() { return flockingMode; }

    public static final class Builder {
        private int entityLevel = Entity.LEVEL_UNCLASSIFIED;
        private boolean canHunt = false;
        private boolean canEatMeat = false;
        private boolean canEatPlants = false;
        private boolean canFlock = false;
        private boolean canHide = false;
        private boolean canBeScared = false;
        private boolean eatOwnSpecies = false;
        private float attackDamagePerSecond = 0.0f;
        private float maxPreySizeMultiplier = 1.5f;
        private FlockingMode flockingMode = FlockingMode.NONE;
        private final Set<Class<? extends Plant>> ediblePlantTypes = new HashSet<>();

        public Builder entityLevel(int entityLevel) {
            this.entityLevel = entityLevel;
            return this;
        }

        public Builder canHunt(boolean canHunt) {
            this.canHunt = canHunt;
            return this;
        }

        public Builder canEatMeat(boolean canEatMeat) {
            this.canEatMeat = canEatMeat;
            return this;
        }

        public Builder canEatPlants(boolean canEatPlants) {
            this.canEatPlants = canEatPlants;
            return this;
        }

        public Builder canFlock(boolean canFlock) {
            this.canFlock = canFlock;
            return this;
        }

        public Builder canHide(boolean canHide) {
            this.canHide = canHide;
            return this;
        }

        public Builder canBeScared(boolean canBeScared) {
            this.canBeScared = canBeScared;
            return this;
        }

        public Builder eatOwnSpecies(boolean eatOwnSpecies) {
            this.eatOwnSpecies = eatOwnSpecies;
            return this;
        }

        public Builder attackDamagePerSecond(float attackDamagePerSecond) {
            this.attackDamagePerSecond = attackDamagePerSecond;
            return this;
        }

        public Builder maxPreySizeMultiplier(float maxPreySizeMultiplier) {
            this.maxPreySizeMultiplier = maxPreySizeMultiplier;
            return this;
        }

        public Builder flockingMode(FlockingMode flockingMode) {
            this.flockingMode = (flockingMode == null) ? FlockingMode.NONE : flockingMode;
            return this;
        }

        @SafeVarargs
        public final Builder ediblePlants(Class<? extends Plant>... plantTypes) {
            this.canEatPlants = true;
            if (plantTypes != null) {
                Collections.addAll(this.ediblePlantTypes, plantTypes);
            }
            return this;
        }

        public AnimalProfile build() {
            return new AnimalProfile(this);
        }
    }
}

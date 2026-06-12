
    public double getHealth() { return vitals != null ? vitals.getHealth() : 0; }
    public void setHealth(double health) { if (vitals != null) vitals.setHealth(health); }
    public double getMaxHealth() { return vitals != null ? vitals.getMaxHealth() : 1; }
    public void setMaxHealth(double maxHealth) { if (vitals != null) vitals.setMaxHealth(maxHealth); }

    public double getHunger() { return vitals != null ? vitals.getHunger() : 0; }
    public void setHunger(double hunger) { if (vitals != null) vitals.setHunger(hunger); }
    public double getMaxHunger() { return vitals != null ? vitals.getMaxHunger() : 1; }
    public void setMaxHunger(double maxHunger) { if (vitals != null) vitals.setMaxHunger(maxHunger); }
    public double getHungerDecayRate() { return vitals != null ? vitals.getHungerDecayRate() : 0; }
    public void setHungerDecayRate(double hungerDecayRate) { if (vitals != null) vitals.setHungerDecayRate(hungerDecayRate); }

    public double getThirst() { return vitals != null ? vitals.getThirst() : 0; }
    public void setThirst(double thirst) { if (vitals != null) vitals.setThirst(thirst); }
    public double getMaxThirst() { return vitals != null ? vitals.getMaxThirst() : 1; }
    public void setMaxThirst(double maxThirst) { if (vitals != null) vitals.setMaxThirst(maxThirst); }
    public double getThirstDecayRate() { return vitals != null ? vitals.getThirstDecayRate() : 0; }
    public void setThirstDecayRate(double thirstDecayRate) { if (vitals != null) vitals.setThirstDecayRate(thirstDecayRate); }

    public double getAge() { return vitals != null ? vitals.getAge() : 0; }
    public void setAge(double age) { if (vitals != null) vitals.setAge(age); }
    public double getMaxAge() { return vitals != null ? vitals.getMaxAge() : 1; }
    public void setMaxAge(double maxAge) { if (vitals != null) vitals.setMaxAge(maxAge); }

    public boolean isAdult() { return vitals != null && vitals.isAdult(); }
    public void setAdult(boolean adult) { if (vitals != null) vitals.setAdult(adult); }

    public double getVisionRange() { return visionRange; }
    public void setVisionRange(double visionRange) { this.visionRange = visionRange; }
}

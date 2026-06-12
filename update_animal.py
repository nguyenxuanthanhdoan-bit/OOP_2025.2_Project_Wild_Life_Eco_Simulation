import re

with open("src/model/living_beings/animal/Animal.java", "r") as f:
    text = f.read()

# Remove field declarations (be careful with regex)
text = re.sub(r"\s*private final Map<UUID, Float> unsafeFoodMemory = new HashMap<\(\)>();", "", text)
text = re.sub(r"\s*private final Map<UUID, DangerZone> dangerZones = new HashMap<\(\)>();", "", text)

# Update update() method
old_update_start = """        updateDangerZones(deltaTime);
        updateUnsafeFoodMemory(deltaTime);
        if (radarCooldown > 0) {
            radarCooldown -= deltaTime;
        }
        if (gardenThreatCooldown > 0) {
            gardenThreatCooldown -= deltaTime;
        }"""
new_update_start = """        if (sensory != null) sensory.update(deltaTime);"""
text = text.replace(old_update_start, new_update_start)

# Replace getters/setters and delegates
text = re.sub(r"public double getVisionRange\(\) \{ return visionRange; \}", "public double getVisionRange() { return sensory != null ? sensory.getVisionRange() : 100; }", text)
text = re.sub(r"public void setVisionRange\(double visionRange\) \{ this\.visionRange = visionRange; \}", "public void setVisionRange(double visionRange) { if (sensory != null) sensory.setVisionRange(visionRange); }", text)

with open("src/model/living_beings/animal/Animal.java", "w") as f:
    f.write(text)

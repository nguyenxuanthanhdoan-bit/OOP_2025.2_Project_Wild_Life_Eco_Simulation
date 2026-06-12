import re

with open("src/model/living_beings/animal/Animal.java", "r") as f:
    text = f.read()

# 1. We replace methods with delegates.
methods_to_replace = {
    r"(?s)\s*/\*\*\s*\* Phát hiện kẻ thù nguy hiểm trong tầm nhìn.*?\*/\s*protected boolean detectDangerousThreats\(\) \{.*?\n    \}": """
    protected boolean detectDangerousThreats() {
        return sensory != null && sensory.detectDangerousThreats();
    }""",
    
    r"(?s)\s*public boolean hasDangerousThreats\(\) \{.*?\n    \}": """
    public boolean hasDangerousThreats() {
        return sensory != null && sensory.hasDangerousThreats();
    }""",

    r"(?s)\s*public boolean hasGardenThreat\(\) \{.*?\n    \}": """
    public boolean hasGardenThreat() {
        return sensory != null && sensory.hasGardenThreat();
    }""",

    r"(?s)\s*public static class DangerZone \{.*?\n    \}\n    private final Map<UUID, DangerZone> dangerZones = new HashMap<\(\)>();": "",
    
    r"(?s)\s*public void markDangerZone\(Entity predator, float radius, float duration\) \{.*?\n    \}": """
    public void markDangerZone(Entity predator, float radius, float duration) {
        if (sensory != null) sensory.markDangerZone(predator, radius, duration);
    }""",

    r"(?s)\s*public boolean isInDangerZone\(Vector2 pos\) \{.*?\n    \}": """
    public boolean isInDangerZone(Vector2 pos) {
        return sensory != null && sensory.isInDangerZone(pos);
    }""",

    r"(?s)\s*private void updateDangerZones\(float deltaTime\) \{.*?\n    \}": "",

    r"(?s)\s*public void markFoodUnsafe\(model\.entity\.Entity food, float duration\) \{.*?\n    \}": """
    public void markFoodUnsafe(model.entity.Entity food, float duration) {
        if (sensory != null) sensory.markFoodUnsafe(food, duration);
    }""",

    r"(?s)\s*public boolean isFoodMarkedUnsafe\(model\.entity\.Entity food\) \{.*?\n    \}": """
    public boolean isFoodMarkedUnsafe(model.entity.Entity food) {
        return sensory != null && sensory.isFoodMarkedUnsafe(food);
    }""",

    r"(?s)\s*private void updateUnsafeFoodMemory\(float deltaTime\) \{.*?\n    \}": ""
}

for pattern, replacement in methods_to_replace.items():
    text = re.sub(pattern, replacement, text)

# Remove the static DangerZone class and dangerZones map since regex might have failed above due to HashMap<>() syntax (it's new HashMap<>())
text = re.sub(r"(?s)\s*public static class DangerZone \{.*?\n    \}\n    private final Map<UUID, DangerZone> dangerZones = new HashMap<>\(\);", "", text)

with open("src/model/living_beings/animal/Animal.java", "w") as f:
    f.write(text)

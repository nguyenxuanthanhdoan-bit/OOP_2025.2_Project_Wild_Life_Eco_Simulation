import re

with open("src/model/living_beings/animal/Animal.java", "r") as f:
    text = f.read()

# Replace the field declaration
text = re.sub(r"\s*private float reproductionCooldown = 0\.0f;\n", "\n", text)

# Insert ReproductionComponent declaration
text = re.sub(r"protected StateComponent state;", "protected StateComponent state;\n    protected ReproductionComponent reproduction;", text)

# Instantiate the component
text = re.sub(r"this\.state = new StateComponent\(this\);", "this.state = new StateComponent(this);\n        this.reproduction = new ReproductionComponent(this);", text)

# Update the `update()` method
text = re.sub(r"reproductionCooldown = Math\.max\(0\.0f, reproductionCooldown - deltaTime\);", "if (reproduction != null) reproduction.update(deltaTime);", text)

# Replace methods with delegates
methods_to_replace = {
    r"(?s)\s*/\*\*\s*Kiểm tra điều kiện đủ để sinh sản\.\s*\*/\s*public boolean canReproduce\(\) \{.*?\n    \}": """
    /** Kiểm tra điều kiện đủ để sinh sản. */
    public boolean canReproduce() {
        return reproduction != null && reproduction.canReproduce();
    }""",

    r"(?s)\s*public void startReproductionCooldown\(\) \{.*?\n    \}": """
    public void startReproductionCooldown() {
        if (reproduction != null) reproduction.startReproductionCooldown();
    }""",

    r"(?s)\s*public boolean canMateWith\(Animal other\) \{.*?\n    \}": """
    public boolean canMateWith(Animal other) {
        return reproduction != null && reproduction.canMateWith(other);
    }"""
}

for pattern, replacement in methods_to_replace.items():
    text = re.sub(pattern, replacement, text)

# Just in case other files accessed reproductionCooldown (it was private so they shouldn't, but let's expose it just in case)
text = text + """
    public float getReproductionCooldown() { return reproduction != null ? reproduction.getReproductionCooldown() : 0.0f; }
    public void setReproductionCooldown(float cd) { if (reproduction != null) reproduction.setReproductionCooldown(cd); }
"""
# Wait, let's inject it cleanly at the end before the last closing brace
text = re.sub(r"\}\s*$", """
    public float getReproductionCooldown() { return reproduction != null ? reproduction.getReproductionCooldown() : 0.0f; }
    public void setReproductionCooldown(float cd) { if (reproduction != null) reproduction.setReproductionCooldown(cd); }
}
""", text)

with open("src/model/living_beings/animal/Animal.java", "w") as f:
    f.write(text)

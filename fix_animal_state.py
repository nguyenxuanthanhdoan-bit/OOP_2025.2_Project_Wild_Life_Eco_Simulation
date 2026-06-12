import re

with open("src/model/living_beings/animal/Animal.java", "r") as f:
    text = f.read()

# Remove fields
text = re.sub(r"\s*protected boolean isMoving;\n", "\n", text)
text = re.sub(r"\s*protected boolean hidden = false;\n", "\n", text)
text = re.sub(r"\s*protected model\.structures\.Bush hiddenInBush = null;\n", "\n", text)
text = re.sub(r"\s*protected String actionState = \"idle\";\n", "\n", text)

# Add StateComponent declaration
text = re.sub(r"protected SensoryComponent sensory;", "protected SensoryComponent sensory;\n    protected StateComponent state;", text)

# Initialize StateComponent
text = re.sub(r"this\.sensory = new SensoryComponent\(this, 100\);", "this.sensory = new SensoryComponent(this, 100);\n        this.state = new StateComponent(this);", text)

# Replace usage in update()
text = re.sub(r"this\.isMoving = distSq > 0\.0001f;", "if (state != null) state.setMoving(distSq > 0.0001f);", text)
text = re.sub(r"float decayMultiplier = this\.isMoving \? 1\.5f : 1\.0f;", "float decayMultiplier = (state != null && state.isMoving()) ? 1.5f : 1.0f;", text)

# Update move()
text = re.sub(r"if \(\!alive \|\| hidden\) return;", "if (!alive || (state != null && state.isHidden())) return;", text)

# Update hideInBush()
old_hideInBush = """    public void hideInBush(model.structures.Bush bush) {
        this.hidden = true;
        this.hiddenInBush = bush;
        this.isMoving = false;
        this.setSpeed(0);
        this.setActionState("idle");
        this.currentVelocity.set(0, 0);
        stuckDetector.reset(); // Reset escape state khi ẩn vào bụi
        if (bush != null) {
            bush.setOccupied(true);
            this.setPosition(bush.getPosition());
        }
    }"""
new_hideInBush = """    public void hideInBush(model.structures.Bush bush) {
        if (state != null) {
            state.setHidden(true);
            state.setHiddenInBush(bush);
            state.setMoving(false);
        }
        this.setSpeed(0);
        this.setActionState("idle");
        this.currentVelocity.set(0, 0);
        stuckDetector.reset(); // Reset escape state khi ẩn vào bụi
        if (bush != null) {
            bush.setOccupied(true);
            this.setPosition(bush.getPosition());
        }
    }"""
text = text.replace(old_hideInBush, new_hideInBush)

# Update exitBush()
old_exitBush = """    public void exitBush() {
        this.hidden = false;
        if (this.hiddenInBush != null) {
            this.hiddenInBush.setOccupied(false);
            this.hiddenInBush = null;
        }
        this.setSpeed(this.baseSpeed);
    }"""
new_exitBush = """    public void exitBush() {
        if (state != null) {
            state.setHidden(false);
            if (state.getHiddenInBush() != null) {
                state.getHiddenInBush().setOccupied(false);
                state.setHiddenInBush(null);
            }
        }
        this.setSpeed(this.baseSpeed);
    }"""
text = text.replace(old_exitBush, new_exitBush)

# Replace accessors and logic methods with delegates
methods_to_replace = {
    r"(?s)\s*public boolean isHidden\(\) \{.*?\n    \}": """
    public boolean isHidden() {
        return state != null && state.isHidden();
    }""",

    r"(?s)\s*public void setHidden\(boolean hidden\) \{.*?\n    \}": """
    public void setHidden(boolean hidden) {
        if (state != null) state.setHidden(hidden);
    }""",

    r"(?s)\s*public String getActionState\(\) \{.*?\n    \}": """
    public String getActionState() {
        return state != null ? state.getActionState() : "idle";
    }""",

    r"(?s)\s*public void setActionState\(String actionState\) \{.*?\n    \}": """
    public void setActionState(String actionState) {
        if (state != null) state.setActionState(actionState);
    }""",

    r"(?s)\s*public String getAnimationState\(\) \{.*?\n    \}": """
    public String getAnimationState() {
        return state != null ? state.getAnimationState() : "idle";
    }""",

    r"(?s)\s*private String resolveLocomotionState\(\) \{.*?\n    \}": "",
    r"(?s)\s*private boolean isSpecialAnimationState\(String state\) \{.*?\n    \}": "",

    r"(?s)\s*public model\.structures\.Bush getHiddenInBush\(\) \{.*?\n    \}": """
    public model.structures.Bush getHiddenInBush() {
        return state != null ? state.getHiddenInBush() : null;
    }""",

    r"(?s)\s*public boolean isMoving\(\) \{ return isMoving; \}": """
    public boolean isMoving() { return state != null && state.isMoving(); }"""
}

for pattern, replacement in methods_to_replace.items():
    text = re.sub(pattern, replacement, text)

# Fix setSpeed to use state delegate for isSpecialAnimationState and resolveLocomotionState
old_setSpeed = """    @Override
    public void setSpeed(float speed) {
        super.setSpeed(Math.max(0.0f, speed));
        if (!isSpecialAnimationState(this.actionState)) {
            this.actionState = resolveLocomotionState();
        }
    }"""
new_setSpeed = """    @Override
    public void setSpeed(float speed) {
        super.setSpeed(Math.max(0.0f, speed));
        if (state != null && !state.isSpecialAnimationState(state.getActionState())) {
            state.setActionState(state.resolveLocomotionState());
        }
    }"""
text = text.replace(old_setSpeed, new_setSpeed)

with open("src/model/living_beings/animal/Animal.java", "w") as f:
    f.write(text)


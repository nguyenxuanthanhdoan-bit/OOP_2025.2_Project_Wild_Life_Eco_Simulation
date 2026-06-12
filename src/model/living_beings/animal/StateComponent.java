package model.living_beings.animal;

import model.structures.Bush;

public class StateComponent {
    private final Animal owner;
    
    private boolean isMoving;
    private boolean hidden = false;
    private Bush hiddenInBush = null;
    private String actionState = "idle";

    public StateComponent(Animal owner) {
        this.owner = owner;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Bush getHiddenInBush() {
        return hiddenInBush;
    }

    public void setHiddenInBush(Bush bush) {
        this.hiddenInBush = bush;
    }

    public String getActionState() {
        return actionState;
    }

    public void setActionState(String actionState) {
        String requestedState = actionState == null ? "idle" : actionState.toLowerCase();
        this.actionState = isSpecialAnimationState(requestedState)
                ? requestedState
                : resolveLocomotionState();
    }

    public String getAnimationState() {
        if (isSpecialAnimationState(actionState)) {
            return actionState;
        }
        return resolveLocomotionState();
    }

    public String resolveLocomotionState() {
        core.GameConfig config = core.GameConfig.getInstance();
        if (owner.getSpeed() <= config.MOVEMENT_SPEED_EPSILON) {
            return "idle";
        }
        return owner.getSpeed() > owner.getBaseSpeed() * config.RUN_ANIMATION_SPEED_MULTIPLIER
                ? "run"
                : "walk";
    }

    public boolean isSpecialAnimationState(String state) {
        return "attack".equals(state)
                || "eat".equals(state)
                || "drink".equals(state)
                || "sleep".equals(state);
    }
}

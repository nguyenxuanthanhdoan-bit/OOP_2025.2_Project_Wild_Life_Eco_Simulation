package model.world;

import model.entity.Entity;

public final class WorldEvent {
    private final WorldEventType type;
    private final Entity entity;
    private final World world;
    private final String reason;

    public WorldEvent(WorldEventType type, Entity entity, World world, String reason) {
        this.type = type;
        this.entity = entity;
        this.world = world;
        this.reason = reason;
    }

    public WorldEventType getType() {
        return type;
    }

    public Entity getEntity() {
        return entity;
    }

    public World getWorld() {
        return world;
    }

    public String getReason() {
        return reason;
    }
}

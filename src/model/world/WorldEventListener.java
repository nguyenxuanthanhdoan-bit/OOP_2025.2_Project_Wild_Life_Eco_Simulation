package model.world;

@FunctionalInterface
public interface WorldEventListener {
    void onEvent(WorldEvent event);
}

package model.world;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WorldEventSystem {
    private final Map<WorldEventType, List<WorldEventListener>> listeners =
            new EnumMap<>(WorldEventType.class);

    public void subscribe(WorldEventType type, WorldEventListener listener) {
        if (type == null || listener == null) return;
        listeners.computeIfAbsent(type, key -> new ArrayList<>()).add(listener);
    }

    public void unsubscribe(WorldEventType type, WorldEventListener listener) {
        if (type == null || listener == null) return;
        List<WorldEventListener> eventListeners = listeners.get(type);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    public void emit(WorldEvent event) {
        if (event == null || event.getType() == null) return;
        List<WorldEventListener> eventListeners = listeners.get(event.getType());
        if (eventListeners == null || eventListeners.isEmpty()) return;

        List<WorldEventListener> snapshot = new ArrayList<>(eventListeners);
        for (WorldEventListener listener : snapshot) {
            listener.onEvent(event);
        }
    }
}

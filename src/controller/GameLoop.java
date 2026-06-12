package controller;

import audio.SoundManager;
import core.GameConfig;
import model.entity.Entity;
import model.living_beings.Animal;
import model.world.World;
import view.systems.Camera;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Quản lý vòng lặp chính của trò chơi.
 * Đảm bảo logic và đồ họa được cập nhật liên tục[cite: 192].
 */
public class GameLoop implements Runnable {

    private final Simulation simulation;
    private final GameConfig config = GameConfig.getInstance();
    private final int targetFPS;
    private volatile boolean isRunning = false;
    private volatile boolean paused = false;
    private float timeScale = 1.0f;
    private Thread thread;
    private Consumer<Float> beforeUpdate = deltaTime -> {};
    private Runnable afterUpdate = () -> {};

    // Sound
    private SoundManager soundManager = null;
    private Camera camera = null;
    private World world = null;

    public GameLoop(Simulation simulation) {
        this.simulation = simulation;
        this.targetFPS = config.TARGET_FPS;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        thread = new Thread(this, "WildLife-GameLoop");
        thread.start();
    }

    public void stop() {
        isRunning = false;
        if (thread == null) return;
        if (Thread.currentThread() == thread) return;
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = Math.max(0.0f, timeScale);
    }

    public float getTimeScale() {
        return timeScale;
    }

    public void setBeforeUpdate(Consumer<Float> beforeUpdate) {
        this.beforeUpdate = beforeUpdate == null ? deltaTime -> {} : beforeUpdate;
    }

    public void setAfterUpdate(Runnable afterUpdate) {
        this.afterUpdate = afterUpdate == null ? () -> {} : afterUpdate;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / targetFPS;

        while (isRunning) {
            long now = System.nanoTime();
            // Tính toán thời gian trôi qua thực tế (giây) [cite: 194]
            float deltaTime = (float) ((now - lastTime) / 1000000000.0);
            lastTime = now;

            // Giới hạn deltaTime để tránh nhảy vọt khi máy bị lag [cite: 194]
            float cappedDeltaTime = Math.min(deltaTime, config.MAX_DELTA_TIME);

            // Cập nhật và vẽ lại thế giới [cite: 195]
            if (!paused) {
                beforeUpdate.accept(cappedDeltaTime);
                simulation.update(cappedDeltaTime * timeScale);
                updateAmbientSound(cappedDeltaTime);
            }
            afterUpdate.run();

            // Nghỉ một chút để đạt target FPS
            try {
                Thread.sleep((long) Math.max(0, (nsPerTick - (System.nanoTime() - now)) / 1000000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

    /**
     * Gọi mỗi frame khi game đang chạy.
     * Quét các động vật trong viewport và ra lệnh phát tiếng ambient nếu đủ điều kiện.
     */
    private void updateAmbientSound(float deltaTime) {
        if (soundManager == null || camera == null || world == null) return;

        float zoom = camera.getZoomLevel();

        // Chỉ quét khi zoom đủ gần
        if (zoom < SoundManager.MIN_ZOOM_FOR_AMBIENT) {
            soundManager.update(deltaTime, zoom, null);
            return;
        }

        // Đếm số động vật trong viewport và tìm loài chiếm đa số
        java.awt.Rectangle viewport = getViewportBounds();
        Map<String, Integer> speciesCount = new HashMap<>();
        int totalAnimals = 0;

        List<Entity> entities;
        try {
            entities = new java.util.ArrayList<>(world.getEntities());
        } catch (Exception e) {
            return;
        }

        for (Entity e : entities) {
            if (!(e instanceof Animal) || !e.isAlive()) continue;
            Animal animal = (Animal) e;

            // Kiểm tra vị trí animal có trong viewport không
            core.Vector2 screenPos = camera.worldToScreen(animal.getPosition());
            if (viewport != null && !viewport.contains((int) screenPos.x, (int) screenPos.y)) continue;

            totalAnimals++;
            String species = animal.getSpeciesName().toLowerCase();
            speciesCount.put(species, speciesCount.getOrDefault(species, 0) + 1);
        }

        // Chỉ phát khi viewport có <= MAX_ANIMALS_FOR_AMBIENT con
        if (totalAnimals == 0 || totalAnimals > SoundManager.MAX_ANIMALS_FOR_AMBIENT) {
            soundManager.update(deltaTime, zoom, null);
            return;
        }

        // Lấy loài có nhiều con nhất trong viewport
        String dominantSpecies = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : speciesCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantSpecies = entry.getKey();
            }
        }

        soundManager.update(deltaTime, zoom, dominantSpecies);
    }

    /**
     * Lấy bộ phận không gian màn hình hiện tại tính theo pixel.
     * Dùng clip bounds của panel làm xấp xỉ.
     */
    private java.awt.Rectangle getViewportBounds() {
        // Ước lượng kích thước viewport từ config (có thể tùy chỉnh sau)
        return new java.awt.Rectangle(0, 0, 1150, 750);
    }

    /** Cung cấp SoundManager và các tham số cần thiết để quét ambient sound. */
    public void setSoundManager(SoundManager soundManager, Camera camera, World world) {
        this.soundManager = soundManager;
        this.camera = camera;
        this.world = world;
    }
}


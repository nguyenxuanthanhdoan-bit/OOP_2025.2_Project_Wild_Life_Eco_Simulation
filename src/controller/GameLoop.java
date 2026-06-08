package controller;

import core.GameConfig;
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
}

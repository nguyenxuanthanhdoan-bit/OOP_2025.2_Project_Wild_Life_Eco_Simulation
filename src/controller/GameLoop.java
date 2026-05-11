package controller;

/**
 * Quản lý vòng lặp chính của trò chơi.
 * Đảm bảo logic và đồ họa được cập nhật liên tục[cite: 192].
 */
public class GameLoop implements Runnable {

    private Simulation simulation;
    private int targetFPS = 60;
    private boolean isRunning = false;
    private Thread thread;

    public GameLoop(Simulation simulation) {
        this.simulation = simulation;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRunning = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / targetFPS;
        float deltaTime = 0;

        while (isRunning) {
            long now = System.nanoTime();
            // Tính toán thời gian trôi qua thực tế (giây) [cite: 194]
            deltaTime = (float) ((now - lastTime) / 1000000000.0);
            lastTime = now;

            // Giới hạn deltaTime để tránh nhảy vọt khi máy bị lag [cite: 194]
            float cappedDeltaTime = Math.min(deltaTime, 0.05f);

            // Cập nhật và vẽ lại thế giới [cite: 195]
            simulation.update(cappedDeltaTime);

            // Nghỉ một chút để đạt target FPS
            try {
                Thread.sleep((long) Math.max(0, (nsPerTick - (System.nanoTime() - now)) / 1000000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
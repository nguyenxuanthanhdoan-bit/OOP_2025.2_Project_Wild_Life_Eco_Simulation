package controller;

import view.systems.Camera;
import core.GameConfig;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputProcessor extends KeyAdapter {

    private Simulation simulation;
    private Camera camera;

    // Lưu trạng thái các phím điều hướng (True = đang đè phím, False = thả phím)
    private boolean up, down, left, right;

    public InputProcessor(Simulation simulation, Camera camera) {
        this.simulation = simulation;
        this.camera = camera;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Bật trạng thái khi đè phím
        if (key == KeyEvent.VK_W) up = true;
        if (key == KeyEvent.VK_S) down = true;
        if (key == KeyEvent.VK_A) left = true;
        if (key == KeyEvent.VK_D) right = true;

        // Các phím chức năng (Bấm 1 lần)
        if (key == KeyEvent.VK_UP) camera.zoom(1.1f);
        if (key == KeyEvent.VK_DOWN) camera.zoom(0.9f);
        if (key == KeyEvent.VK_M) simulation.toggleDisplayMode();
        if (key == KeyEvent.VK_C) simulation.spawnCarcassAtCameraCenter();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        // Tắt trạng thái khi thả phím
        if (key == KeyEvent.VK_W) up = false;
        if (key == KeyEvent.VK_S) down = false;
        if (key == KeyEvent.VK_A) left = false;
        if (key == KeyEvent.VK_D) right = false;
    }

    /**
     * Hàm này sẽ được GameLoop gọi liên tục mỗi frame để di chuyển siêu mượt
     */
    public void updateCameraMovement(float deltaTime) {
        float panSpeed = GameConfig.getInstance().CAMERA_PAN_SPEED * deltaTime * 2.0f;
        float dx = 0, dy = 0;

        if (up) dy -= panSpeed;
        if (down) dy += panSpeed;
        if (left) dx -= panSpeed;
        if (right) dx += panSpeed;

        if (dx != 0 || dy != 0) {
            camera.pan(dx, dy);
        }
    }
}
import controller.GameLoop;
import controller.Simulation;
import controller.InputProcessor;
import model.world.World;
import screen.GameScreen;
import view.Sidebar;
import view.SidebarToggleButton;
import view.systems.Camera;
import view.systems.render.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static void main(String[] args) {
        Camera camera = new Camera(800, 600);
        World world = new World();

        RenderSystem renderSystem = new RenderSystem(camera);
        Simulation simulation = new Simulation(camera, world, renderSystem);
        camera.pan(800, 800);

        JFrame frame = new JFrame("Wild-Life Eco Simulation - SOICT HUST");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 750); // Wider frame to fit right sidebar
        frame.setLocationRelativeTo(null);

        CardLayout cardLayout = new CardLayout();
        JPanel rootPanel = new JPanel(cardLayout);

        // Khởi tạo GameScreen
        GameScreen gameScreen = new GameScreen(simulation);
        final InputProcessor input = new InputProcessor(simulation, camera);
        gameScreen.addKeyListener(input);

        GameLoop gameLoop = new GameLoop(simulation);
        gameLoop.setBeforeUpdate(input::updateCameraMovement);

        // Khởi tạo Sidebar
        Sidebar sidebar = new Sidebar(simulation, gameLoop, gameScreen::requestFocusInWindow);
        gameLoop.setAfterUpdate(() -> {
            gameScreen.repaint();
            sidebar.updateSidebar();
        });

        // Khởi tạo Toggle Button
        SidebarToggleButton toggleButton = new SidebarToggleButton(sidebar, gameScreen::requestFocusInWindow);
        gameScreen.addToggleButton(toggleButton);

        // Parent wrapper panel for simulation screen + right sidebar
        JPanel gameScreenWrapper = new JPanel(new BorderLayout());
        gameScreenWrapper.add(gameScreen, BorderLayout.CENTER);
        gameScreenWrapper.add(sidebar, BorderLayout.EAST);

        // Menu panel transition
        screen.MenuScreen menuPanel = new screen.MenuScreen(e -> {
            cardLayout.show(rootPanel, "GAME");
            gameScreen.requestFocusInWindow();
            gameLoop.start();
        });

        rootPanel.add(menuPanel, "MENU");
        rootPanel.add(gameScreenWrapper, "GAME");

        frame.add(rootPanel);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameLoop.stop();
            }
        });
        frame.setVisible(true);
    }
}

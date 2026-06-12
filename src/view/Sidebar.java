package view;

import controller.GameLoop;
import controller.Simulation;
import javax.swing.*;
import java.awt.*;

public class Sidebar extends JPanel {

    private final Simulation simulation;
    private final GameLoop gameLoop;
    private final Runnable requestFocusCallback;

    private SidebarTogglePanel togglePanel;
    private SidebarControlsPanel controlsPanel;
    private SidebarInfoPanel infoPanel;
    private SidebarStatsPanel statsPanel;

    public Sidebar(Simulation simulation, GameLoop gameLoop, Runnable requestFocusCallback) {
        this.simulation = simulation;
        this.gameLoop = gameLoop;
        this.requestFocusCallback = requestFocusCallback;
        initUI();
    }

    private void initUI() {
        this.setPreferredSize(new Dimension(300, 750));
        this.setBackground(new Color(30, 33, 36));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(50, 55, 60)));

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(new Color(30, 33, 36));
        container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        togglePanel = new SidebarTogglePanel(simulation, requestFocusCallback);
        controlsPanel = new SidebarControlsPanel(simulation, gameLoop, requestFocusCallback);
        infoPanel = new SidebarInfoPanel(simulation);
        statsPanel = new SidebarStatsPanel(simulation);

        container.add(togglePanel);
        container.add(Box.createRigidArea(new Dimension(0, 12)));
        container.add(controlsPanel);
        container.add(Box.createRigidArea(new Dimension(0, 12)));
        container.add(infoPanel);
        container.add(Box.createRigidArea(new Dimension(0, 12)));
        container.add(statsPanel);
        container.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(new Color(30, 33, 36));

        this.add(scrollPane);
    }

    public void updateSidebar() {
        if (infoPanel != null) {
            infoPanel.updateInfo();
        }
        if (statsPanel != null) {
            statsPanel.updateStats();
        }
    }

    @Override
    public void requestFocus() {
        if (requestFocusCallback != null) {
            requestFocusCallback.run();
        }
    }
}

package view;

import controller.GameLoop;
import controller.Simulation;
import model.entity.EntityFactory;
import model.world.World;

import javax.swing.*;
import java.awt.*;

public class SidebarControlsPanel extends JPanel {

    private final Simulation simulation;
    private final GameLoop gameLoop;
    private final Runnable requestFocusCallback;

    public SidebarControlsPanel(Simulation simulation, GameLoop gameLoop, Runnable requestFocusCallback) {
        this.simulation = simulation;
        this.gameLoop = gameLoop;
        this.requestFocusCallback = requestFocusCallback;
        initUI();
    }

    private void initUI() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(new Color(38, 41, 45));
        this.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 60, 65), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("BẢNG ĐIỀU KHIỂN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(90, 185, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(title);

        ImageIcon pauseIcon = UIHelper.loadUiIcon("Pause.png", 20, 20);
        ImageIcon playIcon = UIHelper.loadUiIcon("Play.png", 20, 20);
        JButton pauseBtn = UIHelper.createIconButton("Tạm dừng", pauseIcon, new Dimension(280, 34));
        pauseBtn.addActionListener(e -> {
            boolean isPaused = !gameLoop.isPaused();
            gameLoop.setPaused(isPaused);
            pauseBtn.setText(isPaused ? "Tiếp tục" : "Tạm dừng");
            pauseBtn.setIcon(isPaused ? playIcon : pauseIcon);
            requestFocusCallback.run();
        });

        JPanel speedPanel = new JPanel();
        speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.X_AXIS));
        speedPanel.setBackground(new Color(38, 41, 45));
        speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel timeIcon = new JLabel(UIHelper.loadUiIcon("Time.png", 24, 24));
        JLabel speedLabel = new JLabel("Tốc độ: 1.0x");
        speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        speedLabel.setForeground(new Color(220, 224, 230));
        JButton slowBtn = UIHelper.createSmallButton("-");
        JButton fastBtn = UIHelper.createSmallButton("+");
        Dimension btnDim = new Dimension(42, 24);
        slowBtn.setPreferredSize(btnDim); slowBtn.setMaximumSize(btnDim);
        fastBtn.setPreferredSize(btnDim); fastBtn.setMaximumSize(btnDim);
        
        slowBtn.addActionListener(e -> {
            float ts = gameLoop.getTimeScale();
            if (ts > 0.5f) ts -= 0.5f;
            else if (ts > 0.1f) ts -= 0.1f;
            gameLoop.setTimeScale(ts);
            speedLabel.setText(String.format("Tốc độ: %.1fx", ts));
            requestFocusCallback.run();
        });
        fastBtn.addActionListener(e -> {
            float ts = gameLoop.getTimeScale();
            if (ts < 1.0f) ts += 0.5f;
            else if (ts < 5.0f) ts += 1.0f;
            gameLoop.setTimeScale(ts);
            speedLabel.setText(String.format("Tốc độ: %.1fx", ts));
            requestFocusCallback.run();
        });
        speedPanel.add(timeIcon);
        speedPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        speedPanel.add(speedLabel);
        speedPanel.add(Box.createHorizontalGlue());
        speedPanel.add(slowBtn);
        speedPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        speedPanel.add(fastBtn);

        JPanel spawnPanel = new JPanel();
        spawnPanel.setLayout(new BoxLayout(spawnPanel, BoxLayout.X_AXIS));
        spawnPanel.setBackground(new Color(38, 41, 45));
        spawnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> speciesCombo = new JComboBox<>(EntityFactory.getAvailableEntities());
        speciesCombo.setFocusable(false);
        speciesCombo.setMaximumSize(new Dimension(140, 26));
        speciesCombo.addActionListener(e -> {
            simulation.setSelectedSpawnSpecies((String) speciesCombo.getSelectedItem());
        });

        JLabel hintLabel = new JLabel("(Chuột phải để Spawn)");
        hintLabel.setForeground(new Color(150, 155, 160));
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        spawnPanel.add(speciesCombo);
        spawnPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        spawnPanel.add(hintLabel);

        JButton resetBtn = UIHelper.createIconButton("Reset Thế Giới", UIHelper.loadUiIcon("Replay.png", 20, 20), new Dimension(280, 34));
        resetBtn.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn reset toàn bộ thế giới?", "Xác nhận Reset", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                simulation.reset();
            }
            requestFocusCallback.run();
        });

        JPanel seasonPanel = new JPanel();
        seasonPanel.setLayout(new BoxLayout(seasonPanel, BoxLayout.X_AXIS));
        seasonPanel.setBackground(new Color(38, 41, 45));
        seasonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel seasonLabel = new JLabel("Mùa vụ: ");
        seasonLabel.setForeground(new Color(220, 224, 230));
        JComboBox<String> seasonCombo = new JComboBox<>(new String[]{"Mùa Hè", "Mùa Đông"});
        seasonCombo.setFocusable(false);
        seasonCombo.setMaximumSize(new Dimension(140, 26));
        seasonCombo.addActionListener(e -> {
            if (simulation.getWorld() != null) {
                if (seasonCombo.getSelectedIndex() == 0) {
                    simulation.getWorld().setSeason(World.Season.GROWING);
                } else {
                    simulation.getWorld().setSeason(World.Season.WINTER);
                }
            }
            requestFocusCallback.run();
        });
        seasonPanel.add(seasonLabel);
        seasonPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        seasonPanel.add(seasonCombo);

        this.add(pauseBtn);
        this.add(Box.createRigidArea(new Dimension(0, 8)));
        this.add(speedPanel);
        this.add(Box.createRigidArea(new Dimension(0, 8)));
        this.add(spawnPanel);
        this.add(Box.createRigidArea(new Dimension(0, 8)));
        this.add(seasonPanel);
        this.add(Box.createRigidArea(new Dimension(0, 8)));
        this.add(resetBtn);
    }
}

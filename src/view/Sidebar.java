package view;

import controller.GameLoop;
import controller.Simulation;
import view.systems.Camera;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;

public class Sidebar extends JPanel {

    private final Simulation simulation;
    private final GameLoop gameLoop;
    private final Runnable requestFocusCallback;

    private JLabel infoSpeciesLabel;
    private JLabel infoAgeLabel;
    private JLabel infoHealthLabel;
    private JLabel infoHungerLabel;
    private JLabel infoThirstLabel;
    private JLabel infoActionLabel;
    private JLabel infoStrategyLabel;
    private JPanel infoPanel;
    private JLabel statsLabel;

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

        // 1. TOGGLE SECTION
        JPanel toggleSection = createSectionPanel("CHẾ ĐỘ HIỂN THỊ");
        RenderSystem rs = simulation.getRenderSystem();

        JCheckBox hungerCb = new JCheckBox("Hiện thanh đói", rs.isShowHungerBar());
        JCheckBox thirstCb = new JCheckBox("Hiện thanh khát", rs.isShowThirstBar());
        JCheckBox minimapCb = new JCheckBox("Hiện bản đồ nhỏ", rs.isShowMiniMap());
        JCheckBox speciesCb = new JCheckBox("Hiện tên loài", rs.isShowSpeciesName());
        JCheckBox strategyCb = new JCheckBox("Hiện Strategy/Action", rs.showStrategyLabelAll);
        JCheckBox pathCb = new JCheckBox("Hiện đường đi debug (thực thể được chọn)", rs.isShowDebugPath());
        pathCb.setToolTipText("Chọn một con vật rồi bật ô này để xem đường đi, mục tiêu và tên Strategy của nó");
        JCheckBox visionCb = new JCheckBox("Hiện vùng nhìn AI", rs.isShowAIVision());
        JCheckBox minimapEntitiesCb = new JCheckBox("Đốm thực thể trên minimap", rs.isShowEntitiesOnMinimap());

        JCheckBox[] cbs = {hungerCb, thirstCb, minimapCb, speciesCb, strategyCb, pathCb, visionCb, minimapEntitiesCb};
        for (JCheckBox cb : cbs) {
            cb.setBackground(new Color(38, 41, 45));
            cb.setForeground(new Color(210, 215, 220));
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cb.setFocusable(false);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            toggleSection.add(cb);
        }

        hungerCb.addActionListener(e -> { rs.setShowHungerBar(hungerCb.isSelected()); requestFocus(); });
        thirstCb.addActionListener(e -> { rs.setShowThirstBar(thirstCb.isSelected()); requestFocus(); });
        minimapCb.addActionListener(e -> { rs.setShowMiniMap(minimapCb.isSelected()); requestFocus(); });
        speciesCb.addActionListener(e -> { rs.setShowSpeciesName(speciesCb.isSelected()); requestFocus(); });
        strategyCb.addActionListener(e -> { rs.showStrategyLabelAll = strategyCb.isSelected(); requestFocus(); });
        pathCb.addActionListener(e -> { rs.setShowDebugPath(pathCb.isSelected()); requestFocus(); });
        visionCb.addActionListener(e -> { rs.setShowAIVision(visionCb.isSelected()); requestFocus(); });
        minimapEntitiesCb.addActionListener(e -> { rs.setShowEntitiesOnMinimap(minimapEntitiesCb.isSelected()); requestFocus(); });

        container.add(toggleSection);
        container.add(Box.createRigidArea(new Dimension(0, 12)));

        // 2. CONTROLS SECTION
        JPanel controlSection = createSectionPanel("BẢNG ĐIỀU KHIỂN");

        ImageIcon pauseIcon = loadUiIcon("Pause.png", 20, 20);
        ImageIcon playIcon = loadUiIcon("Play.png", 20, 20);
        JButton pauseBtn = createIconButton("Tạm dừng", pauseIcon, new Dimension(280, 34));
        pauseBtn.addActionListener(e -> {
            boolean isPaused = !gameLoop.isPaused();
            gameLoop.setPaused(isPaused);
            pauseBtn.setText(isPaused ? "Tiếp tục" : "Tạm dừng");
            pauseBtn.setIcon(isPaused ? playIcon : pauseIcon);
            requestFocus();
        });

        JPanel speedPanel = new JPanel();
        speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.X_AXIS));
        speedPanel.setBackground(new Color(38, 41, 45));
        speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel timeIcon = new JLabel(loadUiIcon("Time.png", 24, 24));
        JLabel speedLabel = new JLabel("Tốc độ: 1.0x");
        speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        speedLabel.setForeground(new Color(220, 224, 230));
        JButton slowBtn = createSmallButton("-");
        JButton fastBtn = createSmallButton("+");
        Dimension btnDim = new Dimension(42, 24);
        slowBtn.setPreferredSize(btnDim); slowBtn.setMaximumSize(btnDim);
        fastBtn.setPreferredSize(btnDim); fastBtn.setMaximumSize(btnDim);
        
        slowBtn.addActionListener(e -> {
            float ts = gameLoop.getTimeScale();
            if (ts > 0.5f) ts -= 0.5f;
            else if (ts > 0.1f) ts -= 0.1f;
            gameLoop.setTimeScale(ts);
            speedLabel.setText(String.format("Tốc độ: %.1fx", ts));
            requestFocus();
        });
        fastBtn.addActionListener(e -> {
            float ts = gameLoop.getTimeScale();
            if (ts < 1.0f) ts += 0.5f;
            else if (ts < 5.0f) ts += 1.0f;
            gameLoop.setTimeScale(ts);
            speedLabel.setText(String.format("Tốc độ: %.1fx", ts));
            requestFocus();
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
        JComboBox<String> speciesCombo = new JComboBox<>(new String[]{"Thỏ", "Nai", "Voi", "Sói", "Hổ"});
        speciesCombo.setFocusable(false);
        speciesCombo.setMaximumSize(new Dimension(140, 26));
        JButton spawnBtn = createIconButton("Spawn", loadUiIcon("OK.png", 42, 18), new Dimension(112, 30));
        spawnBtn.addActionListener(e -> {
            String sp = (String) speciesCombo.getSelectedItem();
            Camera camera = simulation.getCamera();
            core.Vector2 camPos = camera.getPosition().copy();
            float zoom = camera.getZoomLevel();
            core.Vector2 spawnPos = new core.Vector2(camPos.x + (800f / zoom) / 2f, camPos.y + (600f / zoom) / 2f);
            spawnPos.x += (Math.random() * 60 - 30);
            spawnPos.y += (Math.random() * 60 - 30);

            model.living_beings.Animal animal = null;
            if ("Thỏ".equals(sp)) animal = new model.living_beings.Rabbit(spawnPos);
            else if ("Nai".equals(sp)) animal = new model.living_beings.Deer(spawnPos, 1);
            else if ("Voi".equals(sp)) animal = new model.living_beings.Elephant(spawnPos, 1);
            else if ("Sói".equals(sp)) animal = new model.living_beings.Wolf(spawnPos);
            else if ("Hổ".equals(sp)) animal = new model.living_beings.Tiger(spawnPos);

            if (animal != null) {
                simulation.getWorld().addEntity(animal);
            }
            requestFocus();
        });
        spawnPanel.add(speciesCombo);
        spawnPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        spawnPanel.add(spawnBtn);

        JButton resetBtn = createIconButton("Reset Thế Giới", loadUiIcon("Replay.png", 20, 20), new Dimension(280, 34));
        resetBtn.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn reset toàn bộ thế giới?", "Xác nhận Reset", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                simulation.reset();
            }
            requestFocus();
        });

        JPanel seasonPanel = new JPanel();
        seasonPanel.setLayout(new BoxLayout(seasonPanel, BoxLayout.X_AXIS));
        seasonPanel.setBackground(new Color(38, 41, 45));
        seasonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel seasonLabel = new JLabel("Mùa vụ: ");
        seasonLabel.setForeground(new Color(220, 224, 230));
        JComboBox<String> seasonCombo = new JComboBox<>(new String[]{"Sinh Trưởng", "Khắc Nghiệt"});
        seasonCombo.setFocusable(false);
        seasonCombo.setMaximumSize(new Dimension(140, 26));
        seasonCombo.addActionListener(e -> {
            if (simulation.getWorld() != null) {
                if (seasonCombo.getSelectedIndex() == 0) {
                    simulation.getWorld().setSeason(model.world.World.Season.GROWING);
                } else {
                    simulation.getWorld().setSeason(model.world.World.Season.WINTER);
                }
            }
            requestFocus();
        });
        seasonPanel.add(seasonLabel);
        seasonPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        seasonPanel.add(seasonCombo);

        controlSection.add(pauseBtn);
        controlSection.add(Box.createRigidArea(new Dimension(0, 8)));
        controlSection.add(speedPanel);
        controlSection.add(Box.createRigidArea(new Dimension(0, 8)));
        controlSection.add(spawnPanel);
        controlSection.add(Box.createRigidArea(new Dimension(0, 8)));
        controlSection.add(seasonPanel);
        controlSection.add(Box.createRigidArea(new Dimension(0, 8)));
        controlSection.add(resetBtn);
        container.add(controlSection);
        container.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. INFO SECTION
        JPanel infoSection = createSectionPanel("THÔNG TIN CHI TIẾT");
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(38, 41, 45));
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoSpeciesLabel = createInfoLabel("Loài: Không có");
        infoAgeLabel = createInfoLabel("Tuổi: -");
        infoHealthLabel = createInfoLabel("Máu: -");
        infoHungerLabel = createInfoLabel("Đói: -");
        infoThirstLabel = createInfoLabel("Khát: -");
        infoActionLabel = createInfoLabel("Hành động: -");
        infoStrategyLabel = createInfoLabel("Chiến thuật: -");

        infoPanel.add(infoSpeciesLabel);
        infoPanel.add(infoAgeLabel);
        infoPanel.add(infoHealthLabel);
        infoPanel.add(infoHungerLabel);
        infoPanel.add(infoThirstLabel);
        infoPanel.add(infoActionLabel);
        infoPanel.add(infoStrategyLabel);

        infoSection.add(infoPanel);
        container.add(infoSection);
        container.add(Box.createRigidArea(new Dimension(0, 12)));

        // 4. STATS SECTION
        JPanel statsSection = createSectionPanel("THỐNG KÊ SỐ LƯỢNG");
        statsLabel = createInfoLabel("Đang đếm...");
        statsSection.add(statsLabel);
        container.add(statsSection);
        
        container.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(new Color(30, 33, 36));

        this.add(scrollPane);
    }

    public void updateSidebar() {
        if (simulation.getRenderSystem() == null) return;

        if (simulation.getWorld() != null) {
            Map<String, Integer> counts = new HashMap<>();
            int total = 0;
            for (model.entity.Entity e : simulation.getWorld().getEntities()) {
                if (e instanceof model.living_beings.Animal && e.isAlive()) {
                    String sp = ((model.living_beings.Animal)e).getSpeciesName();
                    counts.put(sp, counts.getOrDefault(sp, 0) + 1);
                    total++;
                }
            }
            StringBuilder sb = new StringBuilder("<html>");
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>");
            }
            sb.append("<br><b>Tổng số loài: ").append(total).append("</b></html>");
            statsLabel.setText(sb.toString());
        }

        model.living_beings.Animal animal = simulation.getRenderSystem().getSelectedAnimal();
        if (animal != null && animal.isAliveState()) {
            infoSpeciesLabel.setText("Loài: " + animal.getSpeciesName());
            infoAgeLabel.setText(String.format("Tuổi: %.1f / %.1f (%s)", animal.getAge(), animal.getMaxAge(), animal.isAdult() ? "Trưởng thành" : "Trẻ con"));
            infoHealthLabel.setText(String.format("Máu: %.1f%%", (animal.getHealth() / animal.getMaxHealth()) * 100.0));
            infoHungerLabel.setText(String.format("Đói: %.1f%%", (animal.getHunger() / animal.getMaxHunger()) * 100.0));
            infoThirstLabel.setText(String.format("Khát: %.1f%%", (animal.getThirst() / animal.getMaxThirst()) * 100.0));
            infoActionLabel.setText("Hành động: " + animal.getActionState().toUpperCase());
            
            String strategyName = "Không có";
            if (animal.getCurrentStrategy() != null) {
                strategyName = animal.getCurrentStrategy().getClass().getSimpleName();
                // Bỏ chữ 'Strategy' cho gọn
                if (strategyName.endsWith("Strategy")) {
                    strategyName = strategyName.substring(0, strategyName.length() - 8);
                }
            }
            infoStrategyLabel.setText("Chiến thuật: " + strategyName);
        } else {
            infoSpeciesLabel.setText("Loài: Không có (Chưa chọn)");
            infoAgeLabel.setText("Tuổi: -");
            infoHealthLabel.setText("Máu: -");
            infoHungerLabel.setText("Đói: -");
            infoThirstLabel.setText("Khát: -");
            infoActionLabel.setText("Hành động: -");
            infoStrategyLabel.setText("Chiến thuật: -");
        }
    }

    @Override
    public void requestFocus() {
        if (requestFocusCallback != null) {
            requestFocusCallback.run();
        }
    }

    private static JPanel createSectionPanel(String titleText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(38, 41, 45));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 60, 65), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(90, 185, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(title);
        return panel;
    }

    private static ImageIcon loadUiIcon(String fileName, int width, int height) {
        ImageIcon icon = new ImageIcon("resources/assets/images/ui/" + fileName);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static JButton createIconButton(String text, Icon icon, Dimension size) {
        JButton button = new JButton(text, icon);
        button.setFocusable(false);
        button.setForeground(new Color(230, 238, 242));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setIconTextGap(8);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 88, 94), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        button.setMaximumSize(size);
        button.setPreferredSize(size);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private static JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setForeground(new Color(230, 238, 242));
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(80, 88, 94), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(210, 215, 220));
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}

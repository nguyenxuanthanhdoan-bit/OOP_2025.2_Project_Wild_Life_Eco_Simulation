package view;

import controller.Simulation;
import view.systems.RenderSystem;

import javax.swing.*;
import java.awt.*;

public class SidebarTogglePanel extends JPanel {

    private final Simulation simulation;
    private final Runnable requestFocusCallback;

    public SidebarTogglePanel(Simulation simulation, Runnable requestFocusCallback) {
        this.simulation = simulation;
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

        JLabel title = new JLabel("CHẾ ĐỘ HIỂN THỊ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(90, 185, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(title);

        RenderSystem rs = simulation.getRenderSystem();

        JCheckBox hungerCb = new JCheckBox("Hiện thanh đói", rs.isShowHungerBar());
        JCheckBox thirstCb = new JCheckBox("Hiện thanh khát", rs.isShowThirstBar());
        JCheckBox minimapCb = new JCheckBox("Hiện bản đồ nhỏ", rs.isShowMiniMap());
        JCheckBox speciesCb = new JCheckBox("Hiện tên loài", rs.isShowSpeciesName());
        JCheckBox strategyCb = new JCheckBox("Hiện Strategy/Action", rs.showStrategyLabelAll);
        JCheckBox pathCb = new JCheckBox("Hiện đường đi debug (thực thể được chọn)", rs.isShowDebugPath());
        pathCb.setToolTipText("Chọn một con vật rồi bật ô này để xem đường đi, mục tiêu và tên Strategy của nó");
        JCheckBox visionCb = new JCheckBox("Hiện vùng nhìn AI", rs.isShowAIVision());
        JCheckBox healthCb = new JCheckBox("Hiện máu", rs.isShowHealthBar());
        JCheckBox minimapEntitiesCb = new JCheckBox("Đốm thực thể trên minimap", rs.isShowEntitiesOnMinimap());

        JCheckBox[] cbs = {healthCb, hungerCb, thirstCb, minimapCb, speciesCb, strategyCb, pathCb, visionCb, minimapEntitiesCb};
        for (JCheckBox cb : cbs) {
            cb.setBackground(new Color(38, 41, 45));
            cb.setForeground(new Color(210, 215, 220));
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cb.setFocusable(false);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            this.add(cb);
        }

        healthCb.addActionListener(e -> { rs.setShowHealthBar(healthCb.isSelected()); requestFocusCallback.run(); });
        hungerCb.addActionListener(e -> { rs.setShowHungerBar(hungerCb.isSelected()); requestFocusCallback.run(); });
        thirstCb.addActionListener(e -> { rs.setShowThirstBar(thirstCb.isSelected()); requestFocusCallback.run(); });
        minimapCb.addActionListener(e -> { rs.setShowMiniMap(minimapCb.isSelected()); requestFocusCallback.run(); });
        speciesCb.addActionListener(e -> { rs.setShowSpeciesName(speciesCb.isSelected()); requestFocusCallback.run(); });
        strategyCb.addActionListener(e -> { rs.showStrategyLabelAll = strategyCb.isSelected(); requestFocusCallback.run(); });
        pathCb.addActionListener(e -> { rs.setShowDebugPath(pathCb.isSelected()); requestFocusCallback.run(); });
        visionCb.addActionListener(e -> { rs.setShowAIVision(visionCb.isSelected()); requestFocusCallback.run(); });
        minimapEntitiesCb.addActionListener(e -> { rs.setShowEntitiesOnMinimap(minimapEntitiesCb.isSelected()); requestFocusCallback.run(); });
    }
}

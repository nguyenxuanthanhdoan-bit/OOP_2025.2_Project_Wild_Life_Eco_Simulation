package view;

import controller.Simulation;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SidebarStatsPanel extends JPanel {

    private final Simulation simulation;
    private JLabel statsLabel;

    public SidebarStatsPanel(Simulation simulation) {
        this.simulation = simulation;
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

        JLabel title = new JLabel("THỐNG KÊ SỐ LƯỢNG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(90, 185, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(title);

        statsLabel = UIHelper.createInfoLabel("Đang đếm...");
        this.add(statsLabel);
    }

    public void updateStats() {
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
    }
}

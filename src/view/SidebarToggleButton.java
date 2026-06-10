package view;

import javax.swing.*;
import java.awt.*;

public class SidebarToggleButton extends JButton {

    private final JPanel targetSidebar;
    private boolean sidebarVisible = false; // Default requirement: Sidebar = Hidden

    public SidebarToggleButton(JPanel targetSidebar, Runnable requestFocusCallback) {
        super("☰");
        this.targetSidebar = targetSidebar;
        
        // Cấu hình UI cho nút
        this.setFont(new Font("SansSerif", Font.BOLD, 24));
        this.setForeground(Color.WHITE);
        this.setBackground(new Color(45, 50, 55, 200));
        this.setFocusPainted(false);
        this.setBorderPainted(false);
        this.setContentAreaFilled(true);
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.setToolTipText("Bật/Tắt Sidebar");

        // Đảm bảo trạng thái mặc định của sidebar
        if (targetSidebar != null) {
            targetSidebar.setVisible(sidebarVisible);
        }

        this.addActionListener(e -> {
            if (this.targetSidebar != null) {
                sidebarVisible = !sidebarVisible;
                this.targetSidebar.setVisible(sidebarVisible);
                Container parent = this.targetSidebar.getParent();
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }
            }
            if (requestFocusCallback != null) {
                requestFocusCallback.run();
            }
        });
    }
}

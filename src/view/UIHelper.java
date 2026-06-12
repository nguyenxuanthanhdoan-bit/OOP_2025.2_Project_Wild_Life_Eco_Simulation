package view;

import javax.swing.*;
import java.awt.*;

public class UIHelper {

    public static JPanel createSectionPanel(String titleText) {
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

    public static ImageIcon loadUiIcon(String fileName, int width, int height) {
        ImageIcon icon = new ImageIcon("resources/assets/images/ui/" + fileName);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static JButton createIconButton(String text, Icon icon, Dimension size) {
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

    public static JButton createSmallButton(String text) {
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

    public static JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(210, 215, 220));
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}

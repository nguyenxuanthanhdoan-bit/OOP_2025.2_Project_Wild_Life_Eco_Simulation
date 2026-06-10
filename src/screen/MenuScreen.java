package screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class MenuScreen extends JPanel {
    private BufferedImage backgroundImage;
    private ImageIcon playIcon;
    private ImageIcon settingIcon;
    private ImageIcon instructionIcon;
    private ImageIcon playHoverIcon;
    private ImageIcon settingHoverIcon;
    private ImageIcon instructionHoverIcon;

    private JButton playBtn;
    private JButton settingBtn;
    private JButton instructionBtn;

    public MenuScreen(ActionListener onPlayClick) {
        setLayout(null); // Absolute positioning cho dễ đặt các nút
        loadAssets();
        setupButtons(onPlayClick);
    }

    private void loadAssets() {
        try {
            backgroundImage = ImageIO.read(new File("resources/assets/images/ui/screen.png"));
            
            // Đọc icon nút và scale một chút cho vừa
            BufferedImage rawPlay = ImageIO.read(new File("resources/assets/images/ui/Screen_Play.png"));
            BufferedImage rawSetting = ImageIO.read(new File("resources/assets/images/ui/Screen_Setting.png"));
            BufferedImage rawInstruction = ImageIO.read(new File("resources/assets/images/ui/Screen_Instruction.png"));

            // Scale xuống còn khoảng 150px chiều rộng
            int targetWidth = 150;
            playIcon = new ImageIcon(scaleImage(rawPlay, targetWidth));
            settingIcon = new ImageIcon(scaleImage(rawSetting, targetWidth));
            instructionIcon = new ImageIcon(scaleImage(rawInstruction, targetWidth));

            // Hiệu ứng hover: Phóng to lên khoảng 5%
            playHoverIcon = new ImageIcon(scaleImage(rawPlay, (int)(targetWidth * 1.05)));
            settingHoverIcon = new ImageIcon(scaleImage(rawSetting, (int)(targetWidth * 1.05)));
            instructionHoverIcon = new ImageIcon(scaleImage(rawInstruction, (int)(targetWidth * 1.05)));

        } catch (IOException e) {
            System.err.println("Lỗi khi load tài nguyên MenuScreen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Image scaleImage(BufferedImage img, int targetWidth) {
        int height = (int) (img.getHeight() * ((double) targetWidth / img.getWidth()));
        return img.getScaledInstance(targetWidth, height, Image.SCALE_SMOOTH);
    }

    private void setupButtons(ActionListener onPlayClick) {
        playBtn = createButton(playIcon, playHoverIcon);
        settingBtn = createButton(settingIcon, settingHoverIcon);
        instructionBtn = createButton(instructionIcon, instructionHoverIcon);

        // Kích thước của nút bằng với icon gốc
        playBtn.setSize(playIcon.getIconWidth(), playIcon.getIconHeight());
        settingBtn.setSize(settingIcon.getIconWidth(), settingIcon.getIconHeight());
        instructionBtn.setSize(instructionIcon.getIconWidth(), instructionIcon.getIconHeight());

        playBtn.addActionListener(onPlayClick);
        settingBtn.addActionListener(e -> showSettingDialog());
        instructionBtn.addActionListener(e -> showInstructionDialog());

        add(playBtn);
        add(settingBtn);
        add(instructionBtn);
    }

    private JButton createButton(ImageIcon normalIcon, ImageIcon hoverIcon) {
        JButton btn = new JButton(normalIcon);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setIcon(hoverIcon);
                // Dịch lại vị trí một chút để hiệu ứng phóng to lấy tâm ở giữa
                int dw = hoverIcon.getIconWidth() - normalIcon.getIconWidth();
                int dh = hoverIcon.getIconHeight() - normalIcon.getIconHeight();
                btn.setBounds(btn.getX() - dw / 2, btn.getY() - dh / 2, hoverIcon.getIconWidth(), hoverIcon.getIconHeight());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setIcon(normalIcon);
                int dw = hoverIcon.getIconWidth() - normalIcon.getIconWidth();
                int dh = hoverIcon.getIconHeight() - normalIcon.getIconHeight();
                btn.setBounds(btn.getX() + dw / 2, btn.getY() + dh / 2, normalIcon.getIconWidth(), normalIcon.getIconHeight());
            }
        });

        return btn;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // Cập nhật vị trí các nút khi màn hình resize
        int w = getWidth();
        int h = getHeight();
        
        // Đặt 3 nút thành một hàng ngang với khoảng cách lớn hơn để trông thoáng hơn
        int btnSpacing = 120;
        int totalWidth = playBtn.getWidth() + instructionBtn.getWidth() + settingBtn.getWidth() + btnSpacing * 2;
        int startX = (w - totalWidth) / 2;
        int centerY = h * 3 / 4; // Tâm của các nút ở vị trí 3/4 phía dưới màn hình

        playBtn.setLocation(startX, centerY - playBtn.getHeight() / 2);
        instructionBtn.setLocation(startX + playBtn.getWidth() + btnSpacing, centerY - instructionBtn.getHeight() / 2);
        settingBtn.setLocation(startX + playBtn.getWidth() + instructionBtn.getWidth() + btnSpacing * 2, centerY - settingBtn.getHeight() / 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private void showSettingDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Cài Đặt", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 33, 36));
        
        JLabel msg = new JLabel("Bảng Cài Đặt (Đang phát triển)");
        msg.setForeground(Color.WHITE);
        msg.setHorizontalAlignment(SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        panel.add(msg, BorderLayout.CENTER);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showInstructionDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Hướng Dẫn Chơi", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setBackground(new Color(25, 25, 30));

        String htmlContent = "<html><body style='font-family: Arial, sans-serif; padding: 20px; color: #E0E0E0; line-height: 1.6;'>"
                + "<h1 style='color: #4CAF50; text-align: center; border-bottom: 2px solid #4CAF50; padding-bottom: 10px;'>WILD-LIFE ECO SIMULATION</h1>"
                + "<p>Chào mừng bạn đến với mô phỏng sinh thái <b>Wild-Life</b>. Trò chơi này mô phỏng chân thực một chuỗi thức ăn tự nhiên, nơi các loài sinh vật phải tranh giành sự sống, tìm kiếm thức ăn, nước uống và sinh sản để duy trì nòi giống.</p>"
                + "<h2 style='color: #FFC107;'>Cốt truyện</h2>"
                + "<p>Tại một hòn đảo bị cô lập giữa đại dương, các loài động thực vật tự do sinh sôi. Tuy nhiên, sự khắc nghiệt của thời tiết, cái đói khát và sự hiện diện của <i style='color: #FF5252;'>Thú Ăn Thịt</i> khiến cho mọi thứ đều có thể bị tiêu diệt. Bạn sẽ đóng vai trò như một đấng toàn năng (Hoặc một thợ săn) có thể can thiệp vào vòng tuần hoàn này.</p>"
                + "<h2 style='color: #FFC107;'>Cách chơi & Tính năng nổi bật</h2>"
                + "<ul>"
                + "<li><b>Cơ chế Sinh tồn:</b> Mọi sinh vật đều có chỉ số <span style='background-color: #552222; padding: 2px 4px; border-radius: 4px;'>Máu</span>, <span style='background-color: #555522; padding: 2px 4px; border-radius: 4px;'>Đói</span>, và <span style='background-color: #222255; padding: 2px 4px; border-radius: 4px;'>Khát</span>. Chúng sẽ tự động tìm kiếm lá cây, cà rốt hoặc nguồn nước để uống.</li>"
                + "<li><b>Trí tuệ nhân tạo (AI):</b> Động vật tự biết <b>né tránh vật cản</b>, <b>chạy trốn</b> khi gặp kẻ thù, và <b>đi săn</b> khi đói.</li>"
                + "<li><b>Thợ Săn (Hunter):</b> Thợ săn từ làng có thể bắn đạn lửa tiêu diệt các sinh vật nguy hiểm. (Hổ, Sói).</li>"
                + "<li><b>Giao diện Tùy chỉnh:</b> Bật/tắt thanh máu, hiển thị đường di chuyển AI bằng Menu bên phải màn hình.</li>"
                + "</ul>"
                + "<h2 style='color: #FFC107;'>Phím Tắt Điều Khiển</h2>"
                + "<table style='width: 100%; text-align: left; border-collapse: collapse;'>"
                + "<tr><td style='padding: 5px; border: 1px solid #555;'><b>W, A, S, D</b></td><td style='padding: 5px; border: 1px solid #555;'>Di chuyển Camera</td></tr>"
                + "<tr><td style='padding: 5px; border: 1px solid #555;'><b>Nút mũi tên Lên/Xuống</b></td><td style='padding: 5px; border: 1px solid #555;'>Thu / Phóng (Zoom)</td></tr>"
                + "<tr><td style='padding: 5px; border: 1px solid #555;'><b>Phím M</b></td><td style='padding: 5px; border: 1px solid #555;'>Chuyển chế độ màn hình (Chân thực / Radar)</td></tr>"
                + "</table>"
                + "<p style='text-align: center; margin-top: 30px; color: #888;'><i>Chúc bạn có những giây phút thư giãn tuyệt vời!</i></p>"
                + "</body></html>";
        editorPane.setText(htmlContent);
        // Bắt đầu từ đầu trang
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        dialog.setContentPane(scrollPane);
        dialog.setVisible(true);
    }
}

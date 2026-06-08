import controller.GameLoop;
import controller.Simulation;
import controller.InputProcessor;
import model.world.World;
import view.systems.Camera;
import view.systems.RenderSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JPanel {
    private Simulation simulation;

    public Main(Simulation simulation) {
        this.simulation = simulation;
        this.setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        simulation.render(g2d, 0.016f);
    }

    public static void main(String[] args) {
        Camera camera = new Camera(800, 600);
        World world = new World();

        RenderSystem renderSystem = new RenderSystem(camera);
        Simulation simulation = new Simulation(camera, world, renderSystem);
        camera.pan(800, 800);

        JFrame frame = new JFrame("Wild-Life Eco Simulation - SOICT HUST");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        CardLayout cardLayout = new CardLayout();
        JPanel rootPanel = new JPanel(cardLayout);

        Main gamePanel = new Main(simulation);
        final InputProcessor input = new InputProcessor(simulation, camera);
        gamePanel.addKeyListener(input);
        GameLoop gameLoop = new GameLoop(simulation);
        gameLoop.setBeforeUpdate(input::updateCameraMovement);
        gameLoop.setAfterUpdate(gamePanel::repaint);

        // Gắn sự kiện chuyển màn hình vào nút bấm ở Menu
        JPanel menuPanel = createMenuPanel(e -> {
            cardLayout.show(rootPanel, "GAME");
            gamePanel.requestFocusInWindow();
            gameLoop.start();
        });

        rootPanel.add(menuPanel, "MENU");
        rootPanel.add(gamePanel, "GAME");

        frame.add(rootPanel);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameLoop.stop();
            }
        });
        frame.setVisible(true);
    }

    private static JPanel createMenuPanel(ActionListener onStart) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(80, 10, 80));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("WILD-LIFE SIMULATION");
        title.setFont(new Font("Monospaced", Font.BOLD, 45));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = new JButton("BẮT ĐẦU");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        startBtn.addActionListener(onStart);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Bảng hướng dẫn
        JPanel help = new JPanel();
        help.setMaximumSize(new Dimension(400, 150));
        help.setBackground(new Color(0, 0, 0, 150));
        help.setLayout(new GridLayout(3, 1));
        String[] txt = {" WASD: Di chuyển", " Mũi tên: Zoom", " M: Chế độ Radar"};
        for (String s : txt) {
            JLabel l = new JLabel(s);
            l.setForeground(Color.LIGHT_GRAY);
            help.add(l);
        }

        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 40)));
        panel.add(startBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 40)));
        panel.add(help);
        panel.add(Box.createVerticalGlue());

        return panel;
    }
}

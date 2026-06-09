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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Main extends JPanel {
    private Simulation simulation;

    // HUD labels
    private JLabel dayLabel;
    private JLabel seasonLabel;
    private JLabel weatherLabel;
    private JLabel animalsLabel;
    private JLabel plantsLabel;
    private JLabel fpsLabel;

    // JComboBox for season synchronization
    private JComboBox<String> seasonCombo;

    // Inspector labels
    private JLabel infoLabel;
    private JLabel insSpecies;
    private JLabel insAge;
    private JLabel insHP;
    private JLabel insHunger;
    private JLabel insThirst;
    private JLabel insAction;
    private JLabel insStrategy;
    private JLabel insTarget;
    private JLabel insPath;

    // FPS Counter
    private int fps = 0;
    private int fpsCounter = 0;
    private long lastFpsTime = System.currentTimeMillis();

    public Main(Simulation simulation) {
        this.simulation = simulation;
        this.setFocusable(true);

        // Click to inspect animal
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Camera camera = simulation.getCamera();
                core.Vector2 worldClick = camera.screenToWorld(new core.Vector2(e.getX(), e.getY()));

                model.living_beings.Animal nearest = null;
                float bestDist = Float.MAX_VALUE;
                float selectRadius = 40.0f; // radius in world coordinates

                List<model.entity.Entity> entities = new ArrayList<>(simulation.getWorld().getEntities());
                for (model.entity.Entity entity : entities) {
                    if (entity instanceof model.living_beings.Animal && entity.isAlive()) {
                        model.living_beings.Animal animal = (model.living_beings.Animal) entity;
                        float dist = animal.getPosition().distanceTo(worldClick);
                        if (dist < bestDist && dist <= Math.max(selectRadius, animal.getSize() * 1.5f)) {
                            bestDist = dist;
                            nearest = animal;
                        }
                    }
                }

                simulation.getRenderSystem().setSelectedAnimal(nearest);
                Main.this.requestFocusInWindow();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // FPS calculation
        long now = System.currentTimeMillis();
        fpsCounter++;
        if (now - lastFpsTime >= 1000) {
            fps = fpsCounter;
            fpsCounter = 0;
            lastFpsTime = now;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        simulation.render(g2d, 0.016f);

        updateSidebar();
    }

    private void updateSidebar() {
        if (dayLabel == null) return;

        World world = simulation.getWorld();

        // Update HUD labels
        dayLabel.setText("Ngày trong game: " + world.getGameDay());
        seasonLabel.setText("Mùa hiện tại: " + world.getCurrentSeason().getName());
        weatherLabel.setText("Thời tiết: " + world.getCurrentWeather().getName());
        fpsLabel.setText("FPS: " + fps);

        int animalCount = 0;
        int plantCount = 0;
        List<model.entity.Entity> list = new ArrayList<>(world.getEntities());
        for (model.entity.Entity e : list) {
            if (e != null && e.isAlive()) {
                if (e instanceof model.living_beings.Animal) {
                    animalCount++;
                } else if (e instanceof model.plants.Plant || e instanceof model.items.FoodSource) {
                    plantCount++;
                }
            }
        }
        animalsLabel.setText("Tổng số động vật: " + animalCount);
        plantsLabel.setText("Số cây / mồi: " + plantCount);

        // Sync Season dropdown
        if (seasonCombo != null) {
            int currentOrdinal = world.getCurrentSeason().ordinal();
            if (seasonCombo.getSelectedIndex() != currentOrdinal) {
                ActionListener[] listeners = seasonCombo.getActionListeners();
                for (ActionListener l : listeners) seasonCombo.removeActionListener(l);
                seasonCombo.setSelectedIndex(currentOrdinal);
                for (ActionListener l : listeners) seasonCombo.addActionListener(l);
            }
        }

        // Update Inspector labels
        model.living_beings.Animal animal = simulation.getRenderSystem().getSelectedAnimal();
        if (animal == null || !animal.isAliveState()) {
            infoLabel.setVisible(true);
            insSpecies.setVisible(false);
            insAge.setVisible(false);
            insHP.setVisible(false);
            insHunger.setVisible(false);
            insThirst.setVisible(false);
            insAction.setVisible(false);
            insStrategy.setVisible(false);
            insTarget.setVisible(false);
            insPath.setVisible(false);
            if (animal != null) {
                simulation.getRenderSystem().setSelectedAnimal(null);
            }
        } else {
            infoLabel.setVisible(false);
            insSpecies.setVisible(true);
            insAge.setVisible(true);
            insHP.setVisible(true);
            insHunger.setVisible(true);
            insThirst.setVisible(true);
            insAction.setVisible(true);
            insStrategy.setVisible(true);
            insTarget.setVisible(true);
            insPath.setVisible(true);

            insSpecies.setText("Loài: " + animal.getSpeciesName());
            insAge.setText(String.format("Tuổi: %.1f / %.1f (%s)", 
                    animal.getAge(), animal.getMaxAge(), animal.isAdult() ? "Trưởng thành" : "Trẻ con"));
            insHP.setText(String.format("Máu: %.1f%% (%.1f/%.1f)", 
                    (animal.getHealth() / animal.getMaxHealth()) * 100.0, animal.getHealth(), animal.getMaxHealth()));
            insHunger.setText(String.format("Đói: %.1f%% (%.1f/%.1f)", 
                    (animal.getHunger() / animal.getMaxHunger()) * 100.0, animal.getHunger(), animal.getMaxHunger()));
            insThirst.setText(String.format("Khát: %.1f%% (%.1f/%.1f)", 
                    (animal.getThirst() / animal.getMaxThirst()) * 100.0, animal.getThirst(), animal.getMaxThirst()));
            insAction.setText("Hành động: " + animal.getActionState().toUpperCase());
            
            model.strategies.IStrategy strategy = animal.getCurrentStrategy();
            insStrategy.setText("AI Strategy: " + (strategy != null ? strategy.getName() : "None"));
            
            core.Vector2 target = strategy != null ? strategy.getTarget() : null;
            if (target != null) {
                insTarget.setText(String.format("Target: X=%.1f, Y=%.1f", target.x, target.y));
            } else {
                insTarget.setText("Target: Không có");
            }
            
            List<core.Vector2> path = strategy != null ? strategy.getPath() : null;
            if (path != null && !path.isEmpty()) {
                insPath.setText("Đường đi: " + path.size() + " checkpoints còn lại");
            } else {
                insPath.setText("Đường đi: Không có");
            }
        }
    }

    private JPanel createSidebarPanel(GameLoop gameLoop) {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(300, 750));
        sidebar.setBackground(new Color(30, 33, 36));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(50, 55, 60)));

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(new Color(30, 33, 36));
        container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1. HUD SECTION
        JPanel hudSection = createSectionPanel("THÔNG TIN MÔ PHỎNG");
        dayLabel = createValueLabel();
        seasonLabel = createValueLabel();
        weatherLabel = createValueLabel();
        animalsLabel = createValueLabel();
        plantsLabel = createValueLabel();
        fpsLabel = createValueLabel();

        hudSection.add(dayLabel);
        hudSection.add(seasonLabel);
        hudSection.add(weatherLabel);
        hudSection.add(animalsLabel);
        hudSection.add(plantsLabel);
        hudSection.add(fpsLabel);
        container.add(hudSection);

        container.add(Box.createRigidArea(new Dimension(0, 12)));

        // 2. TOGGLE SECTION
        JPanel toggleSection = createSectionPanel("CHẾ ĐỘ HIỂN THỊ");
        RenderSystem rs = simulation.getRenderSystem();

        JCheckBox hungerCb = new JCheckBox("Hiện thanh đói", rs.isShowHungerBar());
        JCheckBox thirstCb = new JCheckBox("Hiện thanh khát", rs.isShowThirstBar());
        JCheckBox minimapCb = new JCheckBox("Hiện bản đồ nhỏ", rs.isShowMiniMap());
        JCheckBox speciesCb = new JCheckBox("Hiện tên loài", rs.isShowSpeciesName());
        JCheckBox pathCb = new JCheckBox("Hiện đường đi debug", rs.isShowDebugPath());
        JCheckBox visionCb = new JCheckBox("Hiện vùng nhìn AI", rs.isShowAIVision());
        JCheckBox minimapEntitiesCb = new JCheckBox("Đốm thực thể trên minimap", rs.isShowEntitiesOnMinimap());

        JCheckBox[] cbs = {hungerCb, thirstCb, minimapCb, speciesCb, pathCb, visionCb, minimapEntitiesCb};
        for (JCheckBox cb : cbs) {
            cb.setBackground(new Color(38, 41, 45));
            cb.setForeground(new Color(210, 215, 220));
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cb.setFocusable(false);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            toggleSection.add(cb);
        }

        hungerCb.addActionListener(e -> rs.setShowHungerBar(hungerCb.isSelected()));
        thirstCb.addActionListener(e -> rs.setShowThirstBar(thirstCb.isSelected()));
        minimapCb.addActionListener(e -> rs.setShowMiniMap(minimapCb.isSelected()));
        speciesCb.addActionListener(e -> rs.setShowSpeciesName(speciesCb.isSelected()));
        pathCb.addActionListener(e -> rs.setShowDebugPath(pathCb.isSelected()));
        visionCb.addActionListener(e -> rs.setShowAIVision(visionCb.isSelected()));
        minimapEntitiesCb.addActionListener(e -> rs.setShowEntitiesOnMinimap(minimapEntitiesCb.isSelected()));

        container.add(toggleSection);

        container.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. CONTROLS SECTION
        JPanel controlSection = createSectionPanel("BẢNG ĐIỀU KHIỂN");

        JButton pauseBtn = new JButton("Tạm dừng");
        pauseBtn.setFocusable(false);
        pauseBtn.setBackground(new Color(220, 53, 69));
        pauseBtn.setForeground(Color.WHITE);
        pauseBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pauseBtn.setMaximumSize(new Dimension(280, 32));
        pauseBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        pauseBtn.addActionListener(e -> {
            boolean isPaused = !gameLoop.isPaused();
            gameLoop.setPaused(isPaused);
            pauseBtn.setText(isPaused ? "Tiếp tục" : "Tạm dừng");
            pauseBtn.setBackground(isPaused ? new Color(40, 167, 69) : new Color(220, 53, 69));
            Main.this.requestFocusInWindow();
        });

        JPanel speedPanel = new JPanel();
        speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.X_AXIS));
        speedPanel.setBackground(new Color(38, 41, 45));
        speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel speedLabel = new JLabel("Tốc độ: 1.0x");
        speedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        speedLabel.setForeground(new Color(220, 224, 230));
        JButton slowBtn = new JButton("-");
        JButton fastBtn = new JButton("+");
        Dimension btnDim = new Dimension(42, 24);
        slowBtn.setPreferredSize(btnDim); slowBtn.setMaximumSize(btnDim); slowBtn.setFocusable(false);
        fastBtn.setPreferredSize(btnDim); fastBtn.setMaximumSize(btnDim); fastBtn.setFocusable(false);
        
        slowBtn.addActionListener(e -> {
            float ts = gameLoop.getTimeScale();
            if (ts > 0.5f) ts -= 0.5f;
            else if (ts > 0.1f) ts -= 0.1f;
            gameLoop.setTimeScale(ts);
            speedLabel.setText(String.format("Tốc độ: %.1fx", ts));
            Main.this.requestFocusInWindow();
        });
        fastBtn.addActionListener(e -> {
            float ts = gameLoop.getTimeScale();
            if (ts < 1.0f) ts += 0.5f;
            else if (ts < 5.0f) ts += 1.0f;
            gameLoop.setTimeScale(ts);
            speedLabel.setText(String.format("Tốc độ: %.1fx", ts));
            Main.this.requestFocusInWindow();
        });
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
        JButton spawnBtn = new JButton("Spawn");
        spawnBtn.setFocusable(false);
        spawnBtn.setBackground(new Color(40, 167, 69));
        spawnBtn.setForeground(Color.WHITE);
        spawnBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
            Main.this.requestFocusInWindow();
        });
        spawnPanel.add(speciesCombo);
        spawnPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        spawnPanel.add(spawnBtn);

        JPanel seasonPanel = new JPanel();
        seasonPanel.setLayout(new BoxLayout(seasonPanel, BoxLayout.X_AXIS));
        seasonPanel.setBackground(new Color(38, 41, 45));
        seasonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel seasonLabelTitle = new JLabel("Đổi mùa: ");
        seasonLabelTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        seasonLabelTitle.setForeground(new Color(220, 224, 230));
        seasonCombo = new JComboBox<>(new String[]{"Xuân", "Hạ", "Thu", "Đông"});
        seasonCombo.setFocusable(false);
        seasonCombo.setMaximumSize(new Dimension(140, 26));
        seasonCombo.addActionListener(e -> {
            int idx = seasonCombo.getSelectedIndex();
            World.Season s = World.Season.values()[idx];
            simulation.getWorld().setSeason(s);
            Main.this.requestFocusInWindow();
        });
        seasonPanel.add(seasonLabelTitle);
        seasonPanel.add(Box.createHorizontalGlue());
        seasonPanel.add(seasonCombo);

        JButton resetBtn = new JButton("Reset Thế Giới");
        resetBtn.setFocusable(false);
        resetBtn.setBackground(new Color(255, 140, 0));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resetBtn.setMaximumSize(new Dimension(280, 32));
        resetBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetBtn.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(Main.this, "Bạn có chắc chắn muốn reset toàn bộ thế giới?", "Xác nhận Reset", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                simulation.reset();
                seasonCombo.setSelectedIndex(simulation.getWorld().getCurrentSeason().ordinal());
            }
            Main.this.requestFocusInWindow();
        });

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

        // 4. INSPECTOR SECTION
        JPanel inspectorSection = createSectionPanel("CHI TIẾT THỰC THỂ");
        infoLabel = createValueLabel();
        infoLabel.setText("Click chuột chọn con vật trên bản đồ");
        insSpecies = createValueLabel();
        insAge = createValueLabel();
        insHP = createValueLabel();
        insHunger = createValueLabel();
        insThirst = createValueLabel();
        insAction = createValueLabel();
        insStrategy = createValueLabel();
        insTarget = createValueLabel();
        insPath = createValueLabel();

        inspectorSection.add(infoLabel);
        inspectorSection.add(insSpecies);
        inspectorSection.add(insAge);
        inspectorSection.add(insHP);
        inspectorSection.add(insHunger);
        inspectorSection.add(insThirst);
        inspectorSection.add(insAction);
        inspectorSection.add(insStrategy);
        inspectorSection.add(insTarget);
        inspectorSection.add(insPath);
        container.add(inspectorSection);

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(new Color(30, 33, 36));

        sidebar.add(scrollPane);
        return sidebar;
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

    private static JLabel createValueLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(220, 224, 230));
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

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

        Main gamePanel = new Main(simulation);
        final InputProcessor input = new InputProcessor(simulation, camera);
        gamePanel.addKeyListener(input);

        GameLoop gameLoop = new GameLoop(simulation);
        gameLoop.setBeforeUpdate(input::updateCameraMovement);
        gameLoop.setAfterUpdate(gamePanel::repaint);

        // Parent wrapper panel for simulation screen + right sidebar
        JPanel gameScreenWrapper = new JPanel(new BorderLayout());
        gameScreenWrapper.add(gamePanel, BorderLayout.CENTER);
        
        JPanel sidebarPanel = gamePanel.createSidebarPanel(gameLoop);
        gameScreenWrapper.add(sidebarPanel, BorderLayout.EAST);

        // Menu panel transition
        JPanel menuPanel = createMenuPanel(e -> {
            cardLayout.show(rootPanel, "GAME");
            gamePanel.requestFocusInWindow();
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

    private static JPanel createMenuPanel(ActionListener onStart) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(30, 10, 45));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("WILD-LIFE SIMULATION");
        title.setFont(new Font("Monospaced", Font.BOLD, 45));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startBtn = new JButton("BẮT ĐẦU");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        startBtn.addActionListener(onStart);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel help = new JPanel();
        help.setMaximumSize(new Dimension(400, 150));
        help.setBackground(new Color(0, 0, 0, 150));
        help.setLayout(new GridLayout(3, 1));
        String[] txt = {" WASD: Di chuyển Camera", " Mũi tên: Thu phóng (Zoom)", " M: Chuyển chế độ Radar / Thực tế"};
        for (String s : txt) {
            JLabel l = new JLabel(s);
            l.setForeground(Color.LIGHT_GRAY);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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

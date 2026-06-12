package view;

import controller.Simulation;
import javax.swing.*;
import java.awt.*;

public class SidebarInfoPanel extends JPanel {

    private final Simulation simulation;
    
    private JLabel infoSpeciesLabel;
    private JLabel infoAgeLabel;
    private JLabel infoHealthLabel;
    private JLabel infoHungerLabel;
    private JLabel infoThirstLabel;
    private JLabel infoActionLabel;
    private JLabel infoStrategyLabel;
    private JLabel infoExtraLabel;

    public SidebarInfoPanel(Simulation simulation) {
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

        JLabel title = new JLabel("THÔNG TIN CHI TIẾT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(90, 185, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(title);

        JPanel infoContainer = new JPanel();
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.Y_AXIS));
        infoContainer.setBackground(new Color(38, 41, 45));
        infoContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoSpeciesLabel = UIHelper.createInfoLabel("Loài: Không có");
        infoAgeLabel = UIHelper.createInfoLabel("Tuổi: -");
        infoHealthLabel = UIHelper.createInfoLabel("Máu: -");
        infoHungerLabel = UIHelper.createInfoLabel("Đói: -");
        infoThirstLabel = UIHelper.createInfoLabel("Khát: -");
        infoActionLabel = UIHelper.createInfoLabel("Hành động: -");
        infoStrategyLabel = UIHelper.createInfoLabel("Chiến thuật: -");
        infoExtraLabel = UIHelper.createInfoLabel("");

        infoContainer.add(infoSpeciesLabel);
        infoContainer.add(infoAgeLabel);
        infoContainer.add(infoHealthLabel);
        infoContainer.add(infoHungerLabel);
        infoContainer.add(infoThirstLabel);
        infoContainer.add(infoActionLabel);
        infoContainer.add(infoStrategyLabel);
        infoContainer.add(infoExtraLabel);

        this.add(infoContainer);
    }

    public void updateInfo() {
        if (simulation.getRenderSystem() == null) return;
        
        model.entity.Entity selectedEntity = simulation.getRenderSystem().getSelectedEntity();
        if (selectedEntity != null && selectedEntity.isAlive()) {
            if (selectedEntity instanceof model.living_beings.Animal) {
                model.living_beings.Animal animal = (model.living_beings.Animal) selectedEntity;
                infoSpeciesLabel.setText("Loài: " + animal.getSpeciesName());
                infoAgeLabel.setText(String.format("Tuổi: %.1f / %.1f (%s)", animal.getAge(), animal.getMaxAge(), animal.isAdult() ? "Trưởng thành" : "Trẻ con"));
                infoHealthLabel.setText(String.format("Máu: %.1f%%", (animal.getHealth() / animal.getMaxHealth()) * 100.0));
                infoHungerLabel.setText(String.format("Đói: %.1f%%", (animal.getHunger() / animal.getMaxHunger()) * 100.0));
                infoThirstLabel.setText(String.format("Khát: %.1f%%", (animal.getThirst() / animal.getMaxThirst()) * 100.0));
                infoActionLabel.setText("Hành động: " + animal.getActionState().toUpperCase());
                
                String strategyName = "Không có";
                if (animal.getCurrentStrategy() != null) {
                    strategyName = animal.getCurrentStrategy().getClass().getSimpleName();
                    if (strategyName.endsWith("Strategy")) {
                        strategyName = strategyName.substring(0, strategyName.length() - 8);
                    }
                }
                infoStrategyLabel.setText("Chiến thuật: " + strategyName);

                if (animal instanceof model.living_beings.Hunter) {
                    model.living_beings.Hunter hunter = (model.living_beings.Hunter) animal;
                    infoExtraLabel.setText(String.format("Đạn: %d | Thức ăn mang: %.1f", hunter.getAmmo(), hunter.getCarriedFood()));
                } else if (animal instanceof model.living_beings.Human) {
                    model.living_beings.Human human = (model.living_beings.Human) animal;
                    infoExtraLabel.setText(String.format("Thức ăn mang: %.1f", human.getCarriedFood()));
                } else {
                    infoExtraLabel.setText("");
                }
            } else if (selectedEntity instanceof model.structures.FoodStorage) {
                model.structures.FoodStorage storage = (model.structures.FoodStorage) selectedEntity;
                infoSpeciesLabel.setText("Loại: Kho Thức Ăn");
                infoAgeLabel.setText("");
                infoHealthLabel.setText("");
                infoHungerLabel.setText("");
                infoThirstLabel.setText("");
                infoActionLabel.setText("");
                infoStrategyLabel.setText("");
                infoExtraLabel.setText(String.format("Lượng thức ăn: %.1f / %.1f", storage.getStoredFood(), storage.getCapacity()));
            } else {
                infoSpeciesLabel.setText("Loại: " + selectedEntity.getClass().getSimpleName());
                infoAgeLabel.setText("");
                infoHealthLabel.setText("");
                infoHungerLabel.setText("");
                infoThirstLabel.setText("");
                infoActionLabel.setText("");
                infoStrategyLabel.setText("");
                infoExtraLabel.setText("");
            }
        } else {
            infoSpeciesLabel.setText("Chưa chọn thực thể nào");
            infoAgeLabel.setText("Tuổi: -");
            infoHealthLabel.setText("Máu: -");
            infoHungerLabel.setText("Đói: -");
            infoThirstLabel.setText("Khát: -");
            infoActionLabel.setText("Hành động: -");
            infoStrategyLabel.setText("Chiến thuật: -");
            infoExtraLabel.setText("");
        }
    }
}

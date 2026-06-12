package view.systems;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class AssetManager {
    private static AssetManager instance;
    private final Map<String, BufferedImage> assetMap;

    private AssetManager() {
        assetMap = new HashMap<>();
        loadAssets();
    }

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    public BufferedImage getAsset(String key) {
        return assetMap.get(key);
    }

    private void loadAssets() {
        String path = "resources/assets/images/";

        // Animals & Fish (Refactored to use registerSpecies)
        String[] herbivores = {"rabbit", "deer", "elephant"};
        for (String sp : herbivores) registerSpecies(sp, "HerbivoreAnimal");

        String[] carnivores = {"tiger", "wolf", "fox"};
        for (String sp : carnivores) registerSpecies(sp, "CarnivoreAnimal");

        String[] fishSpecies = {"clownfish", "shark", "sunfish"};
        for (String sp : fishSpecies) registerSpecies(sp, "Fish");

        loadHumanAssets("human_male", path + "Human/Male/");
        loadHumanAssets("human_female", path + "Human/Female/");
        loadHumanAssets("human_hunter", path + "Human/Hunter/");
        tryLoadAsset("fireball", path + "Human/Hunter/fireball.png");
        tryLoadAsset("dart", path + "Human/Hunter/dart.png");
        tryLoadAsset("heart", path + "ui/heart.png");

        // Plants
        for (int i = 1; i <= 2; i++) tryLoadAsset("grass_" + i, path + "Plant/Grass/Grass_" + i + ".png");
        for (int i = 1; i <= 13; i++) tryLoadAsset("tree_" + i, path + "Plant/Tree/Tree_" + i + ".png");
        tryLoadAsset("tree_winter_1", path + "Plant/Tree/tree_winter_1.png");
        tryLoadAsset("tree_winter_2", path + "Plant/Tree/tree_winter_2.png");
        for (int i = 1; i <= 8; i++) tryLoadAsset("mushroom_" + i, path + "Plant/Mushrooms/Mushroom_" + i + ".png");

        // Structures
        for (int i = 1; i <= 2; i++) tryLoadAsset("bush_" + i, path + "Structures/Bush/Bush_" + i + ".png");
        for (int i = 1; i <= 3; i++) tryLoadAsset("rock_" + i, path + "Structures/Rock/Rock_" + i + ".png");
        
        loadAssetsFromDirectory(path + "village/");
        loadAssetsFromDirectory(path + "desert/");
        loadAssetsFromDirectory(path + "sea/");

        // Items
        for (int i = 1; i <= 2; i++) tryLoadAsset("fruit_" + i, path + "Items/Fruit/Fruit_" + i + ".png");
        tryLoadAsset("meat", path + "Items/Meat/Meat.png");
        tryLoadAsset("bone", path + "Items/Bone/Bone.png");
        tryLoadAsset("egg", path + "Items/egg.png");
        tryLoadAsset("fish", path + "Items/fish.png");
        tryLoadAsset("fishing_net", path + "Items/fishing_net.png");

        // Lanterns
        tryLoadAsset("lantern", path + "Structures/Lantern/lantern.png");
        tryLoadAsset("lantern_2", path + "Structures/Lantern/latern_2.png");
        tryLoadAsset("lantern_3", path + "Structures/Lantern/latern_3.png");
        tryLoadAsset("lantern_bush_1", path + "Structures/Lantern/latern_bush.png");
        tryLoadAsset("lantern_bush_2", path + "Structures/Lantern/latern_bush_2.png");
        tryLoadAsset("lantern_bush_3", path + "Structures/Lantern/latern_bush_3.png");
    }

    public void registerSpecies(String species, String category) {
        if (assetMap.containsKey(species + "_west")) return; // already loaded

        String path = "resources/assets/images/";
        if (category.equals("Fish")) {
            tryLoadAsset(species + "_west", path + "Fish/" + species + ".png");
            return;
        }

        String capitalizedSp = species.substring(0, 1).toUpperCase() + species.substring(1);
        String dirPath = path + category + "/" + capitalizedSp + "/";
        String prefix = species.equals("rabbit") ? "Rabbit_" : (species + "_");
        if (species.equals("fox")) prefix = ""; // Fox img does not have fox_ prefix

        tryLoadAsset(species + "_west", dirPath + "west.png");
        tryLoadAsset(species + "_walk", dirPath + prefix + "walk.png");
        if (!species.equals("elephant")) {
            tryLoadAsset(species + "_run", dirPath + prefix + "run.png");
        }

        if (species.equals("rabbit") || species.equals("deer") || species.equals("wolf")) {
            tryLoadAsset(species + "_eat", dirPath + species + "_eating.png");
        }
        if (species.equals("elephant") || species.equals("tiger") || species.equals("wolf")) {
            tryLoadAsset(species + "_eat", dirPath + species + "_eat.png");
        }
        if (species.equals("fox")) {
            tryLoadAsset(species + "_eat", dirPath + "eat.png");
        }

        tryLoadAsset(species + "_drink", dirPath + species + "_drink.png");
        tryLoadAsset(species + "_drink", dirPath + species + "_drinking.png");
        tryLoadAsset(species + "_sleep", dirPath + "sleep.png");
        tryLoadAsset(species + "_sleep", dirPath + prefix + "sleep.png");

        if (species.equals("tiger")) {
            tryLoadAsset(species + "_attack", dirPath + "tiger_attack.png");
            tryLoadAsset(species + "_drink", dirPath + "tiger_drink.png");
        }
    }

    private void tryLoadAsset(String key, String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                assetMap.put(key, ImageIO.read(f));
            }
        } catch (IOException e) {
            System.err.println("Không thể nạp: " + path);
        }
    }

    private void loadAssetsFromDirectory(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles((parent, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex <= 0) continue;
            String key = name.substring(0, dotIndex).toLowerCase();
            tryLoadAsset(key, file.getPath());
        }
    }

    private void loadHumanAssets(String keyPrefix, String dirPath) {
        tryLoadAsset(keyPrefix + "_idle", dirPath + "idle.png");
        tryLoadAsset(keyPrefix + "_west", dirPath + "idle.png");
        tryLoadAsset(keyPrefix + "_walk", dirPath + "walk.png");
        tryLoadAsset(keyPrefix + "_run", dirPath + "run.png");
        tryLoadAsset(keyPrefix + "_eat", dirPath + "eat.png");
        tryLoadAsset(keyPrefix + "_drink", dirPath + "eat.png");
        tryLoadAsset(keyPrefix + "_attack", dirPath + "fireball.png");
    }
}

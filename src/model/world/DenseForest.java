package model.world;

import core.Vector2;
import core.BiomeType;
import core.DisplayMode;
import model.plants.FruitTree;
import java.util.ArrayList;
import java.util.List;

public class DenseForest extends Biome {
    private List<FruitTree> treeList;

    public DenseForest(Vector2 position, float size) {
        super(position, size, BiomeType.DENSE_FOREST, 0.8f); // Rừng rậm làm chậm tốc độ
        this.treeList = new ArrayList<>();
    }

    public void addTree(FruitTree tree) { treeList.add(tree); }
    public List<FruitTree> getTreeList() { return treeList; }

    @Override
    public void render(DisplayMode mode) {
        // TMX lo việc vẽ nền rừng
    }
}

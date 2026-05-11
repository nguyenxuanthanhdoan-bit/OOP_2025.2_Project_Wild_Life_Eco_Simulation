package world;

import core.Vector2;
import core.BiomeType;
import core.DisplayMode;
import plants.Grass;
import plants.FruitTree;
import java.util.ArrayList;
import java.util.List;

public class Grassland extends Biome {
    private List<Grass> grassList;
    private List<FruitTree> treeList;

    public Grassland(Vector2 position, float size) {
        super(position, size, BiomeType.GRASSLAND, 1.0f);
        this.grassList = new ArrayList<>();
        this.treeList = new ArrayList<>();

        // Sau này Simulation sẽ gọi hàm để tạo (spawn) cây/cỏ ngẫu nhiên ở đây
    }

    public void addGrass(Grass grass) { grassList.add(grass); }
    public void addTree(FruitTree tree) { treeList.add(tree); }

    public List<Grass> getGrassList() { return grassList; }
    public List<FruitTree> getTreeList() { return treeList; }

    @Override
    public void render(DisplayMode mode) {
        // RenderSystem sẽ vẽ nền xanh, sau đó duyệt qua 2 list trên để vẽ cây/cỏ
    }
}
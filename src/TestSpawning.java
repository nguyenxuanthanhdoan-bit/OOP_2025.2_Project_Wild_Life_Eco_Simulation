import model.world.World;
import model.map.GameMap;
import model.world.FishPopulationManager;

public class TestSpawning {
    public static void main(String[] args) {
        World world = new World();
        GameMap map = new GameMap("resources/map/map.tmx");
        world.setGameMap(map);
        
        System.out.println("Entities before update: " + world.getEntities().size());
        
        // This should trigger FishPopulationManager
        world.update(0.016f);
        
        System.out.println("Entities after update: " + world.getEntities().size());
        int fishCount = 0;
        for (model.entity.Entity e : world.getEntities()) {
            if (e instanceof model.living_beings.Fish) fishCount++;
        }
        System.out.println("Fish spawned: " + fishCount);
    }
}

import model.world.World;
import model.map.GameMap;
import model.world.BiomeGenerator;
import model.entity.Entity;
import core.GameConfig;

public class DebugMain {
    public static void main(String[] args) {
        System.out.println("Bắt đầu khởi tạo game...");

        GameMap gameMap = new GameMap("resources/map/map2.tmx");
        World world = new World();
        world.setGameMap(gameMap);

        BiomeGenerator.generateBiomes(world, gameMap);

        System.out.println("Tổng số thực thể trong World: " + world.getEntities().size());
        int fishCount = 0;
        int animalCount = 0;
        for (Entity e : world.getEntities()) {
            if (e instanceof model.living_beings.Fish) {
                fishCount++;
            } else if (e instanceof model.living_beings.Animal) {
                animalCount++;
            }
        }
        System.out.println("Số lượng cá: " + fishCount);
        System.out.println("Số lượng động vật trên cạn: " + animalCount);
        System.out.println("Số lượng grid trong SpatialGrid: " + (world.getSpatialGrid() != null ? "Co" : "Khong"));
    }
}

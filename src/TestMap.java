import model.map.GameMap;

public class TestMap {
    public static void main(String[] args) {
        GameMap map = new GameMap("resources/map/map.tmx");
        System.out.println("Cols: " + map.getCols() + ", Rows: " + map.getRows());
        System.out.println("Layers: " + map.getLayersCount());
        for (GameMap.Tileset ts : map.getTilesets()) {
            System.out.println("Tileset: firstgid=" + ts.firstgid + ", columns=" + ts.columns + ", image=" + (ts.image != null));
        }
    }
}

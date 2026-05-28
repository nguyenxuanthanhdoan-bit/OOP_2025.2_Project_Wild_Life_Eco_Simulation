package model.map;

import core.TileType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class GameMap {
    private TileType[][] grid;
    private int cols;
    private int rows;

    public GameMap(String tmxPath) {
        loadMapFromTMX(tmxPath);
    }

    private void loadMapFromTMX(String path) {
        try {
            File tmxFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(tmxFile);
            doc.getDocumentElement().normalize();

            // 1. ĐỌC KÍCH THƯỚC BẢN ĐỒ
            Element mapElement = (Element) doc.getElementsByTagName("map").item(0);
            this.cols = Integer.parseInt(mapElement.getAttribute("width"));
            this.rows = Integer.parseInt(mapElement.getAttribute("height"));
            this.grid = new TileType[cols][rows];

            // 2. CHỈ ĐỌC DỮ LIỆU ĐỊA HÌNH (LAYER "Ground") ĐỂ VẼ MAP
            parseTileLayer(doc);

            // Ghi chú: Phần parseObjectGroups (đọc Biome) đã được gỡ bỏ ở Phase này.
            // Khi nào bạn code đến Phase làm AI và vùng sinh sản, chúng ta sẽ thêm lại sau.

        } catch (Exception e) {
            System.err.println("Không thể nạp map TMX: " + path);
            e.printStackTrace();
        }
    }

    private void parseTileLayer(Document doc) {
        NodeList layerNodes = doc.getElementsByTagName("layer");
        Element groundLayer = (Element) layerNodes.item(0);

        Element dataElement = (Element) groundLayer.getElementsByTagName("data").item(0);
        String csvData = dataElement.getTextContent().trim();

        // Loại bỏ ký tự xuống dòng và cắt chuỗi CSV
        String[] tileIDs = csvData.replace("\n", "").replace("\r", "").split(",");

        int index = 0;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                String idStr = tileIDs[index].trim();
                int tileID = idStr.isEmpty() ? 0 : Integer.parseInt(idStr);

                grid[x][y] = determineTileTypeFromID(tileID);
                index++;
            }
        }
    }

    private TileType determineTileTypeFromID(int tileID) {
        if (tileID == 0) return TileType.OCEAN;

        // Dựa vào file TMX của bạn để map ID
        if (tileID >= 1 && tileID <= 52) return TileType.OCEAN;
        if (tileID >= 53 && tileID <= 100) return TileType.GRASS;
        if (tileID > 100 && tileID <= 293) return TileType.FOREST;

        return TileType.GRASS;
    }

    // ================= GETTERS =================

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return TileType.OCEAN;
        return grid[x][y];
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
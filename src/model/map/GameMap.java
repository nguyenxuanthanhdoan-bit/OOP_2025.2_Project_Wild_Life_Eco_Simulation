package model.map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.Path2D;

public class GameMap {
    private List<int[][]> layersGrid = new ArrayList<>();
    private int cols;
    private int rows;

    public static class Tileset {
        public int firstgid;
        public BufferedImage image;
        public int tileWidth = 16;
        public int tileHeight = 16;
        public int columns;

        public Tileset(int firstgid, String imagePath) {
            this.firstgid = firstgid;
            try {
                File imgFile = new File(imagePath);

                System.out.println("Đang load: " + imgFile.getAbsolutePath());
                System.out.println("Tồn tại: " + imgFile.exists());

                this.image = ImageIO.read(imgFile);
                this.columns = this.image.getWidth() / this.tileWidth;
            } catch (Exception e) {
                System.err.println("Không thể nạp tileset: " + imagePath);
                e.printStackTrace();
            }
        }
    }

    private List<Tileset> tilesets = new ArrayList<>();

    // Danh sách các đa giác Biome
    public static class MapPolygonObject {
        public String type; // FOREST, PLAIN, OCEAN...
        public Path2D.Float polygonPath;
    }
    private List<MapPolygonObject> biomePolygons = new ArrayList<>();

    // [MỚI] Lưu tên của từng tile layer theo thứ tự để nhận biết loại địa hình
    private List<String> layerNames = new ArrayList<>();
    // Index của layer Water (-1 nếu không tìm thấy)
    private int waterLayerIndex = -1;

    public GameMap(String tmxPath) {
        loadMapFromTmx(tmxPath);
    }

    private void loadMapFromTmx(String path) {
        try {
            File fXmlFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            // Đọc thông số map
            Element mapElement = doc.getDocumentElement();
            this.cols = Integer.parseInt(mapElement.getAttribute("width"));
            this.rows = Integer.parseInt(mapElement.getAttribute("height"));
            int mapTileWidth = Integer.parseInt(mapElement.getAttribute("tilewidth"));
            int mapTileHeight = Integer.parseInt(mapElement.getAttribute("tileheight"));

            // Đọc các tilesets
            NodeList tilesetNodes = doc.getElementsByTagName("tileset");
            for (int i = 0; i < tilesetNodes.getLength(); i++) {
                Element tilesetElement = (Element) tilesetNodes.item(i);
                int firstgid = Integer.parseInt(tilesetElement.getAttribute("firstgid"));
                String source = tilesetElement.getAttribute("source");
                
                // Trích xuất tên file (VD: ../../../../Water_Middle.tsx -> Water_Middle.png)
                String fileName = new File(source).getName().replace(".tsx", ".png");
                String imagePath = "resources/map/" + fileName;
                
                tilesets.add(new Tileset(firstgid, imagePath));
            }

            // Sắp xếp tilesets theo firstgid giảm dần để dễ dàng tra cứu
            tilesets.sort((t1, t2) -> Integer.compare(t2.firstgid, t1.firstgid));

            // Đọc dữ liệu CSV của TẤT CẢ các tile layer
            NodeList layerNodes = doc.getElementsByTagName("layer");
            for (int i = 0; i < layerNodes.getLength(); i++) {
                Element layerElement = (Element) layerNodes.item(i);
                String layerName = layerElement.getAttribute("name");
                layerNames.add(layerName);
                
                // Đánh dấu layer nước
                if ("water".equalsIgnoreCase(layerName)) {
                    waterLayerIndex = layersGrid.size(); // index sau khi add
                }

                NodeList dataList = layerElement.getElementsByTagName("data");
                if (dataList.getLength() > 0) {
                    Element dataElement = (Element) dataList.item(0);
                    String encoding = dataElement.getAttribute("encoding");
                    if ("csv".equals(encoding)) {
                        int[][] layerGrid = new int[cols][rows];
                        String csvData = dataElement.getTextContent().trim();
                        String[] tokens = csvData.split(",");
                        int index = 0;
                        for (int y = 0; y < rows; y++) {
                            for (int x = 0; x < cols; x++) {
                                if (index < tokens.length) {
                                    long rawId = Long.parseLong(tokens[index].trim());
                                    layerGrid[x][y] = (int) rawId;
                                    index++;
                                }
                            }
                        }
                        layersGrid.add(layerGrid);
                    } else {
                        // Layer không có data CSV (ví dụ layer hidden) -> vẫn add placeholder
                        layersGrid.add(new int[cols][rows]);
                    }
                } else {
                    layersGrid.add(new int[cols][rows]);
                }
            }

            // Đọc các Object từ ObjectGroup (hỗ trợ cả tên "Biomes" và "Biome")
            NodeList objectGroupNodes = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroupNodes.getLength(); i++) {
                Element groupElement = (Element) objectGroupNodes.item(i);
                String groupName = groupElement.getAttribute("name");
                if ("Biomes".equalsIgnoreCase(groupName) || "Biome".equalsIgnoreCase(groupName)) {
                    NodeList objectNodes = groupElement.getElementsByTagName("object");
                    for (int j = 0; j < objectNodes.getLength(); j++) {
                        Element objElement = (Element) objectNodes.item(j);
                        String type = objElement.getAttribute("type");
                        if (type == null || type.isEmpty()) {
                            type = objElement.getAttribute("class"); // Tiled mới dùng 'class' thay 'type'
                        }
                        float scaleX = 32.0f / mapTileWidth;
                        float scaleY = 32.0f / mapTileHeight;
                        float x = 0, y = 0;
                        if (!objElement.getAttribute("x").isEmpty()) x = Float.parseFloat(objElement.getAttribute("x")) * scaleX;
                        if (!objElement.getAttribute("y").isEmpty()) y = Float.parseFloat(objElement.getAttribute("y")) * scaleY;

                        NodeList polygonNodes = objElement.getElementsByTagName("polygon");
                        Path2D.Float polyPath = new Path2D.Float();

                        if (polygonNodes.getLength() > 0) {
                            // Dạng đa giác (polygon)
                            Element polygonElement = (Element) polygonNodes.item(0);
                            String pointsStr = polygonElement.getAttribute("points");
                            String[] pointPairs = pointsStr.split(" ");
                            boolean first = true;
                            for (String pair : pointPairs) {
                                String[] coords = pair.split(",");
                                if (coords.length == 2) {
                                    float px = x + Float.parseFloat(coords[0]) * scaleX;
                                    float py = y + Float.parseFloat(coords[1]) * scaleY;
                                    if (first) { polyPath.moveTo(px, py); first = false; }
                                    else { polyPath.lineTo(px, py); }
                                }
                            }
                            polyPath.closePath();
                        } else {
                            // Dạng hình chữ nhật (rectangle) - map2.tmx dùng loại này
                            float w = 0, h = 0;
                            if (!objElement.getAttribute("width").isEmpty()) w = Float.parseFloat(objElement.getAttribute("width")) * scaleX;
                            if (!objElement.getAttribute("height").isEmpty()) h = Float.parseFloat(objElement.getAttribute("height")) * scaleY;
                            polyPath.moveTo(x, y);
                            polyPath.lineTo(x + w, y);
                            polyPath.lineTo(x + w, y + h);
                            polyPath.lineTo(x, y + h);
                            polyPath.closePath();
                        }

                        MapPolygonObject polyObj = new MapPolygonObject();
                        // Map class/type từ Tiled sang tên nội bộ của game
                        polyObj.type = mapBiomeType(type);
                        polyObj.polygonPath = polyPath;
                        biomePolygons.add(polyObj);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Không thể tải map TMX: " + path);
            e.printStackTrace();
        }
    }

    /**
     * Map tên class/type từ Tiled sang tên chuẩn nội bộ của game.
     * FOREST / DenseForest -> "FOREST"
     * PLAIN / Grassland / Plain -> "PLAIN"
     */
    private String mapBiomeType(String rawType) {
        if (rawType == null || rawType.isEmpty()) return "UNKNOWN";
        String lower = rawType.toLowerCase();
        if (lower.contains("forest") || lower.contains("dense")) return "FOREST";
        if (lower.contains("plain") || lower.contains("grass") || lower.contains("land")) return "PLAIN";
        if (lower.contains("ocean") || lower.contains("sea")) return "OCEAN";
        if (lower.contains("lake") || lower.contains("water")) return "LAKE";
        return rawType.toUpperCase();
    }

    public int getLayersCount() {
        return layersGrid.size();
    }

    public int getTileId(int layer, int x, int y) {
        if (layer < 0 || layer >= layersGrid.size()) return 0;
        if (x < 0 || x >= cols || y < 0 || y >= rows) return 0;
        return layersGrid.get(layer)[x][y];
    }

    public List<MapPolygonObject> getBiomePolygons() {
        return biomePolygons;
    }

    /**
     * Kiểm tra vị trí có phải là tile Cầu (Bridge) không.
     * Nếu đúng thì động vật được phép đi qua dù xung quanh là nước.
     */
    public boolean isBridgeTile(float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return false;
        return hasTileOnLayer(col, row, "bridge");
    }

    public boolean isGroundTile(float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return false;
        return hasTileOnLayer(col, row, "ground");
    }

    public boolean isValidGroundSpawnPosition(float worldX, float worldY, float margin) {
        return isGroundTile(worldX, worldY) &&
               isGroundTile(worldX - margin, worldY) &&
               isGroundTile(worldX + margin, worldY) &&
               isGroundTile(worldX, worldY - margin) &&
               isGroundTile(worldX, worldY + margin) &&
               isGroundTile(worldX - margin, worldY - margin) &&
               isGroundTile(worldX + margin, worldY - margin) &&
               isGroundTile(worldX - margin, worldY + margin) &&
               isGroundTile(worldX + margin, worldY + margin) &&
               !isPositionInWater(worldX, worldY) &&
               !isPositionInWater(worldX - margin, worldY) &&
               !isPositionInWater(worldX + margin, worldY) &&
               !isPositionInWater(worldX, worldY - margin) &&
               !isPositionInWater(worldX, worldY + margin) &&
               !isPositionInWater(worldX - margin, worldY - margin) &&
               !isPositionInWater(worldX + margin, worldY - margin) &&
               !isPositionInWater(worldX - margin, worldY + margin) &&
               !isPositionInWater(worldX + margin, worldY + margin);
    }

    private boolean hasTileOnLayer(int col, int row, String layerNamePart) {
        for (int l = 0; l < layersGrid.size(); l++) {
            String lName = (l < layerNames.size()) ? layerNames.get(l).toLowerCase() : "";
            if (lName.contains(layerNamePart)) {
                if ((layersGrid.get(l)[col][row] & 0x0FFFFFFF) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra ô gạch tại vị trí thế giới có phải là nước hay không.
     * Ưu tiên dùng layer có tên "Water" nếu tìm thấy.
     * Nếu không có, fallback về cách kiểm tra tileId cũ (tương thích map.tmx).
     */
    public boolean isWaterTile(float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return true; // ngoài map là nước

        // [MỚI] Nếu có layer Water rõ ràng -> quét từ trên xuống
        if (waterLayerIndex >= 0 && waterLayerIndex < layersGrid.size()) {
            int[][] waterLayer = layersGrid.get(waterLayerIndex);
            boolean hasWaterTile = (waterLayer[col][row] & 0x0FFFFFFF) != 0;
            if (!hasWaterTile) return false; // Ô đó không có gạch Water -> không phải nước

            // Có gạch Water, nhưng kiểm tra xem layer Ground/Sand/Bridge có đè lên không
            for (int l = 0; l < layersGrid.size(); l++) {
                if (l == waterLayerIndex) continue;
                String lName = (l < layerNames.size()) ? layerNames.get(l).toLowerCase() : "";
                // Nếu layer Ground hoặc Sand hoặc Bridge có tile ở đây -> đây là đất
                if (lName.contains("ground") || lName.contains("sand") || lName.contains("bridge")) {
                    if ((layersGrid.get(l)[col][row] & 0x0FFFFFFF) != 0) {
                        return false;
                    }
                }
            }
            return true; // Có nước, không có gì đè -> là nước
        }

        // Fallback: cách cũ cho map.tmx (dùng tileId cứng)
        int rawId0 = layersGrid.get(0)[col][row];
        int tileId0 = rawId0 & 0x0FFFFFFF;
        boolean isWater = (tileId0 == 1 || (tileId0 >= 20 && tileId0 <= 37));
        if (isWater) {
            for (int l = 1; l < layersGrid.size(); l++) {
                int rawId = layersGrid.get(l)[col][row];
                if (rawId != 0) {
                    int tileId = rawId & 0x0FFFFFFF;
                    if ((tileId >= 2 && tileId <= 19) || (tileId >= 38 && tileId < 294)) {
                        return false;
                    }
                }
            }
        }
        return isWater;
    }

    public boolean isPositionInWater(float worldX, float worldY) {
        // 1. Kiểm tra qua Polygon Biome (LAKE, OCEAN)
        for (MapPolygonObject poly : biomePolygons) {
            if ("OCEAN".equalsIgnoreCase(poly.type) || "LAKE".equalsIgnoreCase(poly.type)) {
                if (poly.polygonPath.contains(worldX, worldY)) {
                    return true;
                }
            }
        }
        // 2. Kiểm tra qua Tile Nước
        return isWaterTile(worldX, worldY);
    }

    public List<Tileset> getTilesets() {
        return tilesets;
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}

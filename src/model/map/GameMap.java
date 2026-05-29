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
                this.image = ImageIO.read(new File(imagePath));
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

            // Đọc dữ liệu CSV của TẤT CẢ các layer
            NodeList dataNodes = doc.getElementsByTagName("data");
            for (int i = 0; i < dataNodes.getLength(); i++) {
                Element dataElement = (Element) dataNodes.item(i);
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
                }
            }

            // [MỚI] Đọc các đa giác (Polygons) từ ObjectGroup "Biomes"
            NodeList objectGroupNodes = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroupNodes.getLength(); i++) {
                Element groupElement = (Element) objectGroupNodes.item(i);
                if ("Biomes".equals(groupElement.getAttribute("name"))) {
                    NodeList objectNodes = groupElement.getElementsByTagName("object");
                    for (int j = 0; j < objectNodes.getLength(); j++) {
                        Element objElement = (Element) objectNodes.item(j);
                        String type = objElement.getAttribute("type");
                        float x = 0, y = 0;
                        float scaleX = 32.0f / mapTileWidth;
                        float scaleY = 32.0f / mapTileHeight;

                        if (!objElement.getAttribute("x").isEmpty()) x = Float.parseFloat(objElement.getAttribute("x")) * scaleX;
                        if (!objElement.getAttribute("y").isEmpty()) y = Float.parseFloat(objElement.getAttribute("y")) * scaleY;

                        NodeList polygonNodes = objElement.getElementsByTagName("polygon");
                        if (polygonNodes.getLength() > 0) {
                            Element polygonElement = (Element) polygonNodes.item(0);
                            String pointsStr = polygonElement.getAttribute("points");
                            String[] pointPairs = pointsStr.split(" ");
                            Path2D.Float polyPath = new Path2D.Float();
                            boolean first = true;
                            for (String pair : pointPairs) {
                                String[] coords = pair.split(",");
                                if (coords.length == 2) {
                                    float px = x + Float.parseFloat(coords[0]) * scaleX;
                                    float py = y + Float.parseFloat(coords[1]) * scaleY;
                                    if (first) {
                                        polyPath.moveTo(px, py);
                                        first = false;
                                    } else {
                                        polyPath.lineTo(px, py);
                                    }
                                }
                            }
                            polyPath.closePath();
                            MapPolygonObject polyObj = new MapPolygonObject();
                            polyObj.type = type != null && !type.isEmpty() ? type : "UNKNOWN";
                            polyObj.polygonPath = polyPath;
                            biomePolygons.add(polyObj);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Không thể tải map TMX: " + path);
            e.printStackTrace();
        }
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

    public boolean isWaterTile(float worldX, float worldY) {
        int col = (int) (worldX / 32);
        int row = (int) (worldY / 32);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return true; // ngoài map là nước để cấm đi
        
        // Kiểm tra lớp nền (Ground) - layer 0
        int rawId0 = layersGrid.get(0)[col][row];
        int tileId0 = rawId0 & 0x0FFFFFFF;
        boolean isWater = (tileId0 == 1 || (tileId0 >= 20 && tileId0 <= 37));

        if (isWater) {
            // Nếu nền là nước, kiểm tra xem các lớp Grass, Beach có đè lên không
            for (int l = 1; l < layersGrid.size(); l++) {
                int rawId = layersGrid.get(l)[col][row];
                if (rawId != 0) {
                    int tileId = rawId & 0x0FFFFFFF;
                    // Các tile đất nền: Đường (2-19), Bãi biển (38-52), Cỏ/Cây (53-293)
                    // Nếu gặp các tile này đè lên nước -> đây là đất
                    if ((tileId >= 2 && tileId <= 19) || (tileId >= 38 && tileId < 294)) {
                        return false; 
                    }
                    // Tileset >= 294 là Sunnysideworld có thể chứa bóng râm (shadow),
                    // nếu bóng râm đè lên nước thì đó vẫn là nước, không được phép đi.
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
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
            this.rows = Integer.parseInt(mapElement.getAttribute("height"));

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

    public List<Tileset> getTilesets() {
        return tilesets;
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
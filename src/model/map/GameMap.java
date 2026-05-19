package model.map;

import core.TileType;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GameMap {
    private TileType[][] grid;
    private int cols;
    private int rows;

    public GameMap(String imagePath) {
        loadMapFromImage(imagePath);
    }

    private void loadMapFromImage(String path) {
        try {
            BufferedImage mapImage = ImageIO.read(new File(path));
            this.cols = mapImage.getWidth();
            this.rows = mapImage.getHeight();
            this.grid = new TileType[cols][rows];

            for (int x = 0; x < cols; x++) {
                for (int y = 0; y < rows; y++) {
                    // Lấy màu của từng pixel
                    Color pixelColor = new Color(mapImage.getRGB(x, y));
                    grid[x][y] = determineTileType(pixelColor);
                }
            }
        } catch (IOException e) {
            System.err.println("Không thể tải map: " + path);
            e.printStackTrace();
        }
    }

    // Hàm chuyển đổi màu sắc thành loại đất
    private TileType determineTileType(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        // 1. NƯỚC BIỂN
        if (b > r + 20 && b > g + 20) {
            return TileType.OCEAN;
        }

        // 2. CÁT VÀNG
        if (r > 150 && g > 150 && b < 120) {
            return TileType.SAND;
        }

        // 3. NÚI ĐÁ (Bắt chính xác tọa độ màu #46655d với dung sai 15)
        // R=70, G=101, B=93
        if (Math.abs(r - 70) <= 15 && Math.abs(g - 101) <= 15 && Math.abs(b - 93) <= 15) {
            return TileType.MOUNTAIN;
        }

        // 4. RỪNG ĐẬM
        if (g > r && g > b && g < 120) {
            return TileType.FOREST;
        }

        // 5. CỎ (Mặc định)
        return TileType.GRASS;
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return TileType.OCEAN;
        return grid[x][y];
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
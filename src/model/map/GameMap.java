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

        // 1. Nhận diện NƯỚC BIỂN (Màu xanh dương chiếm ưu thế)
        if (b > r && b > g) {
            return TileType.OCEAN;
        }

        // 2. Nhận diện CÁT (Màu vàng: Đỏ và Xanh lá đều cao, Xanh dương thấp)
        if (r > 150 && g > 150 && b < 120) {
            return TileType.SAND;
        }

        // 3. Nhận diện RỪNG ĐẬM (Màu xanh lá tối màu)
        if (r < 100 && g < 120 && b < 100) {
            return TileType.FOREST;
        }

        // 4. Nhận diện NÚI ĐÁ (Màu xám: 3 dải màu gần bằng nhau)
        if (Math.abs(r - g) < 20 && Math.abs(g - b) < 20 && r < 120) {
            return TileType.MOUNTAIN;
        }

        // 5. MẶC ĐỊNH: Nếu không phải 4 loại trên, quy hết về CỎ
        return TileType.GRASS;
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return TileType.OCEAN;
        return grid[x][y];
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
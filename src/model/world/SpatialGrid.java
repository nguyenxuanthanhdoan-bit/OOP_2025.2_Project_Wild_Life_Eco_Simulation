package model.world;

import core.Vector2;
import model.entity.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * Cấu trúc dữ liệu Lưới không gian (Spatial Grid).
 * Phân chia thế giới thành các ô nhỏ để tối ưu hóa truy vấn không gian (va chạm, render).
 */
public class SpatialGrid {
    private final float cellSize;
    private final int cols;
    private final int rows;

    // Mảng 2 chiều chứa danh sách các Entity
    private final List<Entity>[][] grid;

    @SuppressWarnings("unchecked")
    public SpatialGrid(float worldWidth, float worldHeight, float cellSize) {
        this.cellSize = cellSize;
        this.cols = (int) Math.ceil(worldWidth / cellSize);
        this.rows = (int) Math.ceil(worldHeight / cellSize);

        // Khởi tạo mảng hai chiều
        this.grid = new ArrayList[cols][rows];
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                this.grid[x][y] = new ArrayList<>();
            }
        }
    }

    // --- HÀM HELPER (Dùng nội bộ để tránh lặp code) ---

    private int getCellX(float x) {
        return (int) (x / cellSize);
    }

    private int getCellY(float y) {
        return (int) (y / cellSize);
    }

    private boolean isValidCell(int cellX, int cellY) {
        return cellX >= 0 && cellX < cols && cellY >= 0 && cellY < rows;
    }

    // --- CÁC HÀM TƯƠNG TÁC CHÍNH ---

    /**
     * Thêm một thực thể vào đúng ô lưới.
     */
    public void add(Entity entity) {
        if (entity == null || entity.getPosition() == null) return;

        int cellX = getCellX(entity.getPosition().x);
        int cellY = getCellY(entity.getPosition().y);

        if (isValidCell(cellX, cellY)) {
            grid[cellX][cellY].add(entity);
        }
    }

    /**
     * Xóa thực thể khỏi ô lưới (khi bị tiêu diệt hoặc thu hoạch).
     */
    public void remove(Entity entity) {
        if (entity == null || entity.getPosition() == null) return;

        int cellX = getCellX(entity.getPosition().x);
        int cellY = getCellY(entity.getPosition().y);

        if (isValidCell(cellX, cellY)) {
            grid[cellX][cellY].remove(entity);
        }
    }

    /**
     * Cập nhật vị trí của thực thể trên lưới khi nó di chuyển.
     * Chỉ thực hiện chuyển List nếu thực thể thực sự bước sang ô lưới khác.
     */
    public void updateEntityPosition(Entity entity, Vector2 oldPos) {
        if (entity == null || oldPos == null || entity.getPosition() == null) return;

        int oldCellX = getCellX(oldPos.x);
        int oldCellY = getCellY(oldPos.y);

        int newCellX = getCellX(entity.getPosition().x);
        int newCellY = getCellY(entity.getPosition().y);

        // Nếu khác ô, thực hiện việc di chuyển Entity giữa 2 List
        if (oldCellX != newCellX || oldCellY != newCellY) {
            if (isValidCell(oldCellX, oldCellY)) {
                grid[oldCellX][oldCellY].remove(entity);
            }
            if (isValidCell(newCellX, newCellY)) {
                grid[newCellX][newCellY].add(entity);
            }
        }
    }

    /**
     * Lấy danh sách tất cả các thực thể nằm trong phạm vi nhất định xung quanh một tọa độ.
     * Tối ưu hóa cực mạnh cho việc Render và Tìm kiếm mục tiêu của AI.
     */
    public List<Entity> getNeighbors(Vector2 position, float range) {
        List<Entity> neighbors = new ArrayList<>();
        if (position == null) return neighbors;

        // Tính toán các mốc quét giới hạn
        int startX = getCellX(position.x - range);
        int endX = getCellX(position.x + range);
        int startY = getCellY(position.y - range);
        int endY = getCellY(position.y + range);

        // Chặn biên để không vượt ra ngoài mảng
        startX = Math.max(0, startX);
        endX = Math.min(cols - 1, endX);
        startY = Math.max(0, startY);
        endY = Math.min(rows - 1, endY);

        // Gom các thực thể lại
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                neighbors.addAll(grid[x][y]);
            }
        }
        return neighbors;
    }

    // --- GETTER ---

    public float getCellSize() {
        return cellSize;
    }

    public void clear() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                grid[x][y].clear();
            }
        }
    }
}
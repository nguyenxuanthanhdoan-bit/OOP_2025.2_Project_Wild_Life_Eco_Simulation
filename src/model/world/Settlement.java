package model.world;

import core.Vector2;
import model.structures.House;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đại diện cho một khu dân cư (Human Settlement).
 *
 * Mỗi Settlement:
 *   - Có một vùng Safe Zone hình tròn (circle) bao quanh khu làng.
 *   - Quản lý danh sách các ngôi nhà thuộc khu dân cư đó.
 *   - Cung cấp API tìm ngôi nhà gần nhất với một vị trí cho trước.
 *
 * Động vật nguy hiểm (Wolf, Tiger, Elephant, Deer) KHÔNG được phép
 * di chuyển vào vùng này — kiểm tra qua {@link #isInsideSafeZone(Vector2)}.
 */
public class Settlement {

    private final Vector2 center;
    private final float safeRadius;
    private final List<House> houses = new ArrayList<>();

    /**
     * @param center     Tâm của khu dân cư (thường là clusterCenter của polygon làng)
     * @param safeRadius Bán kính vùng cấm động vật nguy hiểm
     */
    public Settlement(Vector2 center, float safeRadius) {
        this.center = center == null ? new Vector2(0, 0) : center.copy();
        this.safeRadius = Math.max(0, safeRadius);
    }

    // =========================================================
    // QUẢN LÝ DANH SÁCH NHÀ
    // =========================================================

    /**
     * Đăng ký một ngôi nhà vào khu dân cư này.
     */
    public void addHouse(House house) {
        if (house != null && !houses.contains(house)) {
            houses.add(house);
        }
    }

    /**
     * Trả về danh sách nhà (read-only).
     */
    public List<House> getHouses() {
        return Collections.unmodifiableList(houses);
    }

    // =========================================================
    // API TÌM NHÀ
    // =========================================================

    /**
     * Tìm ngôi nhà gần nhất với vị trí cho trước.
     * Ưu tiên nhà còn chỗ trống (hasSpace()), nếu không thì trả nhà gần nhất bất kỳ.
     *
     * @param position Vị trí cần tìm nhà gần nhất
     * @return Nhà gần nhất, hoặc {@code null} nếu khu dân cư chưa có nhà nào.
     */
    public House findNearestHouse(Vector2 position) {
        if (position == null || houses.isEmpty()) return null;

        House bestWithSpace = null;
        House bestAny = null;
        float distBestSpace = Float.MAX_VALUE;
        float distBestAny = Float.MAX_VALUE;

        for (House house : houses) {
            if (!house.isAlive()) continue;
            float dist = position.distanceTo(house.getPosition());

            if (dist < distBestAny) {
                distBestAny = dist;
                bestAny = house;
            }
            if (house.hasSpace() && dist < distBestSpace) {
                distBestSpace = dist;
                bestWithSpace = house;
            }
        }

        return bestWithSpace != null ? bestWithSpace : bestAny;
    }

    // =========================================================
    // SAFE ZONE
    // =========================================================

    /**
     * Kiểm tra xem vị trí {@code pos} có nằm trong vùng Safe Zone của khu dân cư không.
     *
     * @param pos Vị trí cần kiểm tra (world coordinates)
     * @return {@code true} nếu nằm trong vòng tròn safeRadius quanh center
     */
    public boolean isInsideSafeZone(Vector2 pos) {
        if (pos == null) return false;
        return center.distanceTo(pos) <= safeRadius;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Vector2 getCenter() {
        return center.copy();
    }

    public float getSafeRadius() {
        return safeRadius;
    }

    public int getHouseCount() {
        return houses.size();
    }

    @Override
    public String toString() {
        return String.format("Settlement[center=(%.0f,%.0f) radius=%.0f houses=%d]",
                center.x, center.y, safeRadius, houses.size());
    }
}

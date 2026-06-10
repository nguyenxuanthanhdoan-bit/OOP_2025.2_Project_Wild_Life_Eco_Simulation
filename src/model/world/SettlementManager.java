package model.world;

import core.Vector2;
import model.structures.House;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SettlementManager — Quản lý toàn bộ khu dân cư trên bản đồ.
 *
 * Trách nhiệm:
 *   1. Lưu trữ danh sách {@link Settlement} được tạo ra khi khởi tạo map.
 *   2. Cung cấp API tìm nhà gần nhất với bất kỳ vị trí nào trên bản đồ.
 *   3. Kiểm tra xem một vị trí có nằm trong vùng cấm động vật nguy hiểm không.
 *
 * Vòng đời:
 *   - {@link model.world.BiomeGenerator} gọi {@link #addSettlement(Settlement)}
 *     sau khi spawn xong từng village.
 *   - {@link model.world.World} giữ một instance duy nhất và reset khi world reset.
 *
 * Thiết kế:
 *   - Không hard-code vị trí nhà hay bán kính safe zone.
 *   - Mọi thông số đến từ GameConfig hoặc được tính động từ polygon của map.
 */
public class SettlementManager {

    private final List<Settlement> settlements = new ArrayList<>();

    // =========================================================
    // QUẢN LÝ SETTLEMENT
    // =========================================================

    /**
     * Đăng ký một khu dân cư mới.
     * Được gọi bởi {@link model.world.BiomeGenerator} sau khi spawn village xong.
     */
    public void addSettlement(Settlement settlement) {
        if (settlement != null) {
            settlements.add(settlement);
        }
    }

    /**
     * Xóa toàn bộ settlement (dùng khi reset world).
     */
    public void clear() {
        settlements.clear();
    }

    /**
     * Trả về danh sách tất cả settlement (read-only).
     */
    public List<Settlement> getSettlements() {
        return Collections.unmodifiableList(settlements);
    }

    public int getSettlementCount() {
        return settlements.size();
    }

    // =========================================================
    // API TÌM NHÀ
    // =========================================================

    /**
     * Tìm ngôi nhà gần nhất với vị trí cho trước, trên toàn bộ bản đồ.
     *
     * Tìm qua tất cả settlement → ưu tiên nhà còn chỗ trống.
     *
     * @param position Vị trí cần tìm nhà gần nhất (world coordinates)
     * @return Ngôi nhà gần nhất còn trống, hoặc nhà gần nhất bất kỳ,
     *         hoặc {@code null} nếu chưa có nhà nào trên bản đồ.
     */
    public House findNearestHouse(Vector2 position) {
        if (position == null || settlements.isEmpty()) return null;

        House bestWithSpace = null;
        House bestAny = null;
        float distBestSpace = Float.MAX_VALUE;
        float distBestAny   = Float.MAX_VALUE;

        for (Settlement settlement : settlements) {
            for (House house : settlement.getHouses()) {
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
        }

        return bestWithSpace != null ? bestWithSpace : bestAny;
    }

    /**
     * Tìm Settlement chứa vị trí cho trước (tâm gần nhất trong vòng safeRadius).
     *
     * @param pos Vị trí cần kiểm tra
     * @return Settlement tương ứng, hoặc {@code null} nếu vị trí không nằm trong bất kỳ settlement nào.
     */
    public Settlement findSettlementContaining(Vector2 pos) {
        if (pos == null) return null;
        for (Settlement s : settlements) {
            if (s.isInsideSafeZone(pos)) return s;
        }
        return null;
    }

    // =========================================================
    // SAFE ZONE CHECK
    // =========================================================

    /**
     * Kiểm tra nhanh xem vị trí {@code pos} có nằm trong bất kỳ vùng
     * Safe Zone (vùng cấm động vật nguy hiểm) nào không.
     *
     * Được gọi bởi {@link model.world.World#isValidPositionFor()} để reject
     * movement của động vật nguy hiểm vào làng.
     *
     * @param pos Vị trí cần kiểm tra (world coordinates)
     * @return {@code true} nếu nằm trong vùng cấm của ít nhất 1 settlement
     */
    public boolean isInsideSettlement(Vector2 pos) {
        if (pos == null || settlements.isEmpty()) return false;
        for (Settlement s : settlements) {
            if (s.isInsideSafeZone(pos)) return true;
        }
        return false;
    }
}

package model.world;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Human;
import model.structures.Boat;
import model.structures.FishingHut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * CoastalManager — Quản lý các điểm thu hút ven biển cho Human.
 *
 * Lưu trữ danh sách thuyền và nhà chài; cung cấp API để
 * PassiveStrategy (AI ban ngày) lấy ra một điểm ngẫu nhiên gần biển
 * để Human hướng tới.
 *
 * Vòng đời:
 *   - BiomeGenerator gọi {@link #addBoat} / {@link #addFishingHut} khi spawn.
 *   - PassiveStrategy gọi {@link #randomCoastalPoint} để chọn đích lang thang.
 *   - World gọi {@link #clear} khi reset.
 */
public class CoastalManager {

    private final List<Boat> boats = new ArrayList<>();
    private final List<FishingHut> fishingHuts = new ArrayList<>();
    private final Random random = new Random();

    // =========================================================
    // ĐĂNG KÝ
    // =========================================================

    public void addBoat(Boat boat) {
        if (boat != null) boats.add(boat);
    }

    public void addFishingHut(FishingHut hut) {
        if (hut != null) fishingHuts.add(hut);
    }

    public void clear() {
        boats.clear();
        fishingHuts.clear();
    }

    // =========================================================
    // API CHO AI
    // =========================================================

    /**
     * Trả về danh sách tất cả Points of Interest ven biển (thuyền + nhà chài).
     */
    public List<Entity> getAllCoastalPOIs() {
        List<Entity> all = new ArrayList<>();
        all.addAll(boats);
        all.addAll(fishingHuts);
        return all;
    }

    /**
     * Trả về một điểm ngẫu nhiên ven biển (vị trí của thuyền hoặc nhà chài).
     * Ưu tiên nhà chài vì nằm trên đất (Human có thể đến được).
     * Thuyền trả về vị trí của chúng — Human sẽ đứng ở bờ gần đó vì nước chặn.
     *
     * @return Vị trí ngẫu nhiên, hoặc {@code null} nếu chưa có POI nào.
     */
    public Vector2 randomCoastalPoint() {
        List<Entity> candidates = new ArrayList<>();

        // Ưu tiên nhà chài (80% khả năng nếu có)
        if (!fishingHuts.isEmpty() && (boats.isEmpty() || random.nextFloat() < 0.8f)) {
            candidates.addAll(fishingHuts);
        } else if (!boats.isEmpty()) {
            candidates.addAll(boats);
        } else if (!fishingHuts.isEmpty()) {
            candidates.addAll(fishingHuts);
        }

        if (candidates.isEmpty()) return null;

        Entity chosen = candidates.get(random.nextInt(candidates.size()));
        return chosen.isAlive() ? chosen.getPosition().copy() : null;
    }

    /**
     * Tìm nhà chài gần nhất với vị trí cho trước.
     */
    public FishingHut findNearestFishingHut(Vector2 position) {
        if (position == null || fishingHuts.isEmpty()) return null;
        FishingHut best = null;
        float bestDist = Float.MAX_VALUE;
        for (FishingHut hut : fishingHuts) {
            if (!hut.isAlive()) continue;
            float d = position.distanceTo(hut.getPosition());
            if (d < bestDist) { bestDist = d; best = hut; }
        }
        return best;
    }

    public Boat reserveAvailableBoat(Human fisherman) {
        if (fisherman == null) return null;

        Boat best = null;
        float bestDist = Float.MAX_VALUE;
        for (Boat boat : boats) {
            if (!boat.isAlive() || !boat.canBoard()) continue;
            float dist = fisherman.getPosition().distanceTo(boat.getPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = boat;
            }
        }
        return best != null && best.reserveSeat(fisherman) ? best : null;
    }

    public void releaseBoatReservation(Boat boat, Human fisherman) {
        if (boat != null) boat.releaseReservation(fisherman);
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public List<Boat> getBoats() { return Collections.unmodifiableList(boats); }
    public List<FishingHut> getFishingHuts() { return Collections.unmodifiableList(fishingHuts); }
    public boolean hasCoastalPOIs() { return !boats.isEmpty() || !fishingHuts.isEmpty(); }
}

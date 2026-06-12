package model.living_beings;

import model.living_beings.animal.Animal;

import core.Vector2;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FishFactory {
    // Map lưu trữ các hàm khởi tạo Fish dựa theo tên loài
    private static final Map<String, Function<Vector2, Fish>> registry = new HashMap<>();

    static {
        // Đăng ký các loài cá biển
        register("Shark", pos -> new Shark(pos, new OceanOnlyRule()));
        register("Sunfish", pos -> new Sunfish(pos, new OceanOnlyRule()));

        // Đăng ký các loài cá nước ngọt (hồ)
        register("Bluetang", pos -> new Bluetang(pos, new FreshWaterRule()));
        register("Clownfish", pos -> new Clownfish(pos, new FreshWaterRule()));
    }

    public static void register(String speciesName, Function<Vector2, Fish> creator) {
        registry.put(speciesName, creator);
    }

    public static Fish create(String speciesName, Vector2 position) {
        Function<Vector2, Fish> creator = registry.get(speciesName);
        if (creator != null) {
            return creator.apply(position);
        }
        System.err.println("Không tìm thấy loài cá nào có tên: " + speciesName);
        return null;
    }
}

package model.entity;

import core.Vector2;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import model.living_beings.*;
import model.structures.*;
import model.plants.*;

public class EntityFactory {
    
    // Dùng LinkedHashMap để giữ nguyên thứ tự khi hiển thị trên Sidebar
    private static final Map<String, Function<Vector2, Entity>> REGISTRY = new LinkedHashMap<>();

    static {
        // --- ĐỘNG VẬT ---
        REGISTRY.put("Thỏ", pos -> new Rabbit(pos));
        REGISTRY.put("Nai", pos -> new Deer(pos, 1));
        REGISTRY.put("Voi", pos -> new Elephant(pos, 1));
        REGISTRY.put("Sói", pos -> new Wolf(pos));
        REGISTRY.put("Hổ", pos -> new Tiger(pos));
        REGISTRY.put("Cáo", pos -> new Fox(pos));
        
        // --- CÁ ---
        REGISTRY.put("Cá hề", pos -> FishFactory.create("Clownfish", pos));
        REGISTRY.put("Cá xanh", pos -> FishFactory.create("Bluetang", pos));
        REGISTRY.put("Cá mặt trăng", pos -> FishFactory.create("Sunfish", pos));
        REGISTRY.put("Cá mập", pos -> FishFactory.create("Shark", pos));

        // --- CON NGƯỜI ---
        REGISTRY.put("Dân làng Nam", pos -> new Human(pos, Human.Variant.MALE));
        REGISTRY.put("Dân làng Nữ", pos -> new Human(pos, Human.Variant.FEMALE));
        REGISTRY.put("Thợ săn", pos -> new Hunter(pos));

        // --- CÔNG TRÌNH ---
        REGISTRY.put("Nhà ở", pos -> new House(pos, 1));
        REGISTRY.put("Thuyền", pos -> new Boat(pos));
        REGISTRY.put("Kho thức ăn", pos -> new FoodStorage(pos));
        REGISTRY.put("Nhà chài", pos -> new FishingHut(pos));
        REGISTRY.put("Luống rau", pos -> new GardenBed(pos));
        REGISTRY.put("Giếng nước", pos -> new Well(pos, 1));
        REGISTRY.put("Đèn lồng", pos -> new Lantern(pos, "lantern_1"));
        REGISTRY.put("Bụi cây", pos -> new Bush(pos));
        REGISTRY.put("Đá", pos -> new Rock(pos));

        // --- THỰC VẬT ---
        REGISTRY.put("Cây ăn quả", pos -> new FruitTree(pos));
        REGISTRY.put("Cỏ", pos -> new Grass(pos));
        REGISTRY.put("Xương rồng", pos -> new Cactus(pos));
        REGISTRY.put("Nấm", pos -> new Mushroom(pos));
        REGISTRY.put("Rong biển", pos -> new Seaweed(pos));
        REGISTRY.put("Cỏ khô", pos -> new Straw(pos));
    }

    /**
     * Hàm dùng cho Sidebar: Lấy toàn bộ tên các thực thể đã đăng ký
     */
    public static String[] getAvailableEntities() {
        return REGISTRY.keySet().toArray(new String[0]);
    }

    /**
     * Hàm dùng cho GameScreen: Yêu cầu tạo ra object dựa vào tên
     */
    public static Entity createEntity(String name, Vector2 pos) {
        Function<Vector2, Entity> constructor = REGISTRY.get(name);
        if (constructor != null) {
            return constructor.apply(pos);
        }
        System.err.println("EntityFactory: Không tìm thấy thực thể có tên " + name);
        return null;
    }
}

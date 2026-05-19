package model.structures;

import core.Vector2;
import core.StructureType;
import model.entity.Entity;
import model.entity.Structure;

/**
 * Lớp đại diện cho thực thể Ngôi nhà (House) trong khu vực Làng mạc.
 * Đóng vai trò là vật cản cứng đối với động vật và là nơi trú ẩn an toàn cho Con người.
 */
public class House extends Structure {

    public House(Vector2 position) {
        // Tọa độ khởi tạo của ngôi nhà
        // Kích thước chuẩn: 32f (khớp với kích thước ô đất TILE_SIZE)
        // StructureType: HOUSE
        // isWalkable = false: Vật cản cứng, thú dữ (Sói, Hổ) hoặc thực thể động không thể đi xuyên qua
        // isHideout = true: Nơi ẩn nấp, giúp Con người kích hoạt trạng thái tàng hình khi đi vào
        super(position, 32f, StructureType.HOUSE, false, true);
    }

    /**
     * Sửa đổi logic tương tác khi có một thực thể tiếp cận hoặc đi vào ngôi nhà.
     */
    @Override
    public void onInteract(Entity actor) {
        // Logic chi tiết sẽ được người phụ trách AI/Logic hoàn thiện ở các nhánh sau:
        // - Nếu actor là Con người (Human) đang bị truy đuổi -> Kích hoạt trạng thái ẩn nấp (ẩn hình).
        // - Khi đang ẩn nấp trong nhà, thực thể sẽ liên tục check Kho lương thực chung để xoa dịu cơn đói.
        // - Nếu actor là Thú dữ (Sói, Hổ) -> Không thể truy cập, buộc phải bỏ mục tiêu và rời đi.

        System.out.println(actor.getClass().getSimpleName() + " đang tương tác với Ngôi nhà tại tọa độ: " + getPosition());
    }
}
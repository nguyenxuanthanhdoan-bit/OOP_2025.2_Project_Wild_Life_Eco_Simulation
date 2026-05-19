package model.entity;

import core.Vector2;
import core.StructureType;
import core.DisplayMode;
/**
 * Lớp trừu tượng đại diện cho TẤT CẢ các vật thể tĩnh (Công trình, cây cối, đá, vật phẩm rớt...)
 */
public abstract class Structure extends Entity {

    protected StructureType structureType;
    protected boolean isWalkable; // Động vật có thể đi xuyên qua không? (Đá/Nhà = false, Cỏ/Bụi rậm = true)
    protected boolean isHideout;  // Có phải nơi tàng hình không? (Nhà cho người, Bụi rậm cho thỏ = true)

    public Structure(Vector2 position, float size, StructureType type, boolean isWalkable, boolean isHideout) {
        // Gọi hàm khởi tạo của Entity cha
        super(position, size);
        this.structureType = type;
        this.isWalkable = isWalkable;
        this.isHideout = isHideout;
    }

    /**
     * Ghi đè hàm update của Entity.
     * Mặc định vật thể tĩnh sẽ đứng im để tiết kiệm CPU.
     * Các class con như Cỏ hoặc Trái Cây rụng có thể ghi đè (Override) lại hàm này để làm logic đếm thời gian thối rữa/sinh sôi.
     */
    @Override
    public void update(float deltaTime) {
        // Mặc định không làm gì cả
    }

    /**
     * Hàm bắt buộc các class con phải viết logic: Khi có một thực thể khác (actor) chạm vào thì điều gì xảy ra?
     */
    public abstract void onInteract(Entity actor);

    // --- Getters ---
    public StructureType getStructureType() { return structureType; }
    public boolean isWalkable() { return isWalkable; }
    public boolean isHideout() { return isHideout; }
    @Override
    public void render(DisplayMode mode) {
        // Không làm gì cả, nhường sân khấu cho RenderSystem
    }
}
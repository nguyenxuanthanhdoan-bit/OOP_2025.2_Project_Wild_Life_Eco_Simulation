package model.collision;

/**
 * Giao diện đại diện cho bất kỳ đối tượng nào có thể xảy ra va chạm.
 * Khuyến khích sử dụng thay cho việc kiểm tra instanceof cụ thể từng class.
 */
public interface Collidable {
    /**
     * Trả về Collider của đối tượng này.
     * @return Collider, hoặc null nếu không có va chạm.
     */
    Collider getCollider();
    
    /**
     * Trả về cờ đánh dấu liệu đối tượng này có phải là vật cản cứng hay không.
     */
    boolean isSolid();
}

package core;

/**
 * Xác định chế độ hiển thị của hệ thống (RenderSystem).
 * Hỗ trợ chuyển đổi qua lại giữa đồ họa tối giản và đồ họa chân thực
 * mà không làm ảnh hưởng đến logic sinh thái bên dưới.
 */
public enum DisplayMode {

    /**
     * Chế độ Tối giản (Hình học cơ bản).
     * Render các thực thể bằng hình khối đơn sắc (VD: Thỏ là hình vuông màu lam).
     * Dành cho góc nhìn tổng thể (Zoom out) hoặc khi tua nhanh thời gian (Fast-forward)
     * để tránh quá tải đồ họa.
     */
    MINIMAL,

    /**
     * Chế độ Đồ họa chân thực (Ảnh động sinh động).
     * Render các thực thể bằng hình ảnh thật (Sprites/Textures).
     * Dành cho góc nhìn cận cảnh, mô phỏng giống một bộ phim tài liệu.
     */
    REALISTIC
}
package vn.iotstar.service;

import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.ThongBao;

import java.util.List;

/**
 * Service cho hệ thống thông báo (Notification).
 * Kết hợp với ThongBaoWebSocketService để push realtime.
 */
public interface ThongBaoService {

    /** Lấy 10 thông báo mới nhất cho dropdown header */
    List<ThongBao> getRecentNotifications(NguoiDung nguoiNhan);

    /** Lấy toàn bộ thông báo (trang xem tất cả) */
    List<ThongBao> getAllNotifications(NguoiDung nguoiNhan);

    /** Đếm số thông báo chưa đọc (hiển thị badge đỏ) */
    long countUnread(NguoiDung nguoiNhan);

    /** Đánh dấu tất cả là đã đọc */
    void markAllAsRead(NguoiDung nguoiNhan);

    /** Đánh dấu một thông báo là đã đọc */
    void markAsRead(Integer maThongBao, NguoiDung nguoiNhan);

    /** Xoá tất cả thông báo đã đọc */
    void deleteReadNotifications(NguoiDung nguoiNhan);

    /**
     * Tạo và persist một thông báo mới.
     * ThongBaoWebSocketService sẽ gọi phương thức này sau khi push WS.
     */
    ThongBao createNotification(NguoiDung nguoiNhan, String tieuDe,
                                 String noiDung, String loai,
                                 String duongDan, String icon, String mauSac);

    // ── Helper shortcut methods ──────────────────────────────────────

    /** Thông báo đơn hàng đổi trạng thái */
    ThongBao notifyOrderStatus(NguoiDung nguoiNhan, Integer maDatHang, String trangThaiMoi);

    /** Thông báo vendor nhận đơn hàng mới */
    ThongBao notifyNewOrder(NguoiDung vendor, Integer maDatHang, String tenKhachHang);

    /** Thông báo khuyến mãi mới */
    ThongBao notifyPromotion(NguoiDung nguoiNhan, String tenCuaHang, String tenKhuyenMai);

    /** Thông báo hệ thống từ Admin */
    ThongBao notifySystem(NguoiDung nguoiNhan, String noiDung);
}

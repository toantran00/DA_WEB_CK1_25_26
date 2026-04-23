package vn.iotstar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.ThongBao;
import vn.iotstar.repository.ThongBaoRepository;
import vn.iotstar.service.ThongBaoService;

import java.util.List;
import java.util.Map;

/**
 * Notification service — persist to DB + push realtime via WebSocket.
 *
 * WebSocket destination: /user/{email}/queue/notifications
 * Client nhận qua: SockJS + STOMP subscribe("/user/queue/notifications")
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThongBaoServiceImpl implements ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Core CRUD ──────────────────────────────────────────────────

    @Override
    public List<ThongBao> getRecentNotifications(NguoiDung nguoiNhan) {
        return thongBaoRepository.findTop10ByNguoiNhanOrderByThoiGianTaoDesc(nguoiNhan);
    }

    @Override
    public List<ThongBao> getAllNotifications(NguoiDung nguoiNhan) {
        return thongBaoRepository.findByNguoiNhanOrderByThoiGianTaoDesc(nguoiNhan);
    }

    @Override
    public long countUnread(NguoiDung nguoiNhan) {
        return thongBaoRepository.countByNguoiNhanAndDaDoc(nguoiNhan, false);
    }

    @Override
    @Transactional
    public void markAllAsRead(NguoiDung nguoiNhan) {
        thongBaoRepository.markAllAsRead(nguoiNhan);
    }

    @Override
    @Transactional
    public void markAsRead(Integer maThongBao, NguoiDung nguoiNhan) {
        thongBaoRepository.markOneAsRead(maThongBao, nguoiNhan);
    }

    @Override
    @Transactional
    public void deleteReadNotifications(NguoiDung nguoiNhan) {
        thongBaoRepository.deleteReadNotifications(nguoiNhan);
    }

    @Override
    @Transactional
    public ThongBao createNotification(NguoiDung nguoiNhan, String tieuDe,
                                        String noiDung, String loai,
                                        String duongDan, String icon, String mauSac) {
        ThongBao thongBao = ThongBao.builder()
                .nguoiNhan(nguoiNhan)
                .tieuDe(tieuDe)
                .noiDung(noiDung)
                .loai(loai)
                .duongDan(duongDan)
                .icon(icon)
                .mauSac(mauSac)
                .build();

        ThongBao saved = thongBaoRepository.save(thongBao);

        // Push realtime qua WebSocket tới user cụ thể
        try {
            messagingTemplate.convertAndSendToUser(
                    nguoiNhan.getEmail(),
                    "/queue/notifications",
                    Map.of(
                        "maThongBao", saved.getMaThongBao(),
                        "tieuDe",    saved.getTieuDe(),
                        "noiDung",   saved.getNoiDung(),
                        "loai",      saved.getLoai(),
                        "duongDan",  saved.getDuongDan() != null ? saved.getDuongDan() : "#",
                        "icon",      saved.getIcon(),
                        "mauSac",    saved.getMauSac(),
                        "thoiGian",  saved.getThoiGianTao().toString()
                    )
            );
            log.debug("Pushed notification via WebSocket to user: {}", nguoiNhan.getEmail());
        } catch (Exception e) {
            // WebSocket push failure không nên làm hỏng transaction
            log.warn("Could not push WebSocket notification to {}: {}", nguoiNhan.getEmail(), e.getMessage());
        }

        return saved;
    }

    // ── Shortcut helpers ───────────────────────────────────────────

    @Override
    public ThongBao notifyOrderStatus(NguoiDung nguoiNhan, Integer maDatHang, String trangThaiMoi) {
        String icon;
        String color;
        switch (trangThaiMoi) {
            case "Xác nhận"      -> { icon = "bi bi-check-circle";  color = "success"; }
            case "Đang giao"     -> { icon = "bi bi-truck";          color = "primary"; }
            case "Hoàn thành"    -> { icon = "bi bi-bag-check";      color = "success"; }
            case "Hủy"           -> { icon = "bi bi-x-circle";       color = "danger";  }
            default              -> { icon = "bi bi-info-circle";    color = "info";    }
        }
        return createNotification(
                nguoiNhan,
                "Cập nhật đơn hàng #" + maDatHang,
                "Đơn hàng của bạn đã được cập nhật sang trạng thái: " + trangThaiMoi,
                "ORDER_STATUS",
                "/orders/" + maDatHang,
                icon, color
        );
    }

    @Override
    public ThongBao notifyNewOrder(NguoiDung vendor, Integer maDatHang, String tenKhachHang) {
        return createNotification(
                vendor,
                "Đơn hàng mới #" + maDatHang,
                "Bạn có đơn hàng mới từ khách hàng " + tenKhachHang + ". Hãy xác nhận ngay!",
                "NEW_ORDER",
                "/vendor/orders/" + maDatHang,
                "bi bi-bag-plus", "warning"
        );
    }

    @Override
    public ThongBao notifyPromotion(NguoiDung nguoiNhan, String tenCuaHang, String tenKhuyenMai) {
        return createNotification(
                nguoiNhan,
                "🎉 Ưu đãi mới từ " + tenCuaHang,
                "Cửa hàng " + tenCuaHang + " vừa tung mã ưu đãi: " + tenKhuyenMai + ". Mua ngay!",
                "PROMOTION",
                "/store/" + tenCuaHang,
                "bi bi-tag", "warning"
        );
    }

    @Override
    public ThongBao notifySystem(NguoiDung nguoiNhan, String noiDung) {
        return createNotification(
                nguoiNhan,
                "Thông báo hệ thống",
                noiDung,
                "SYSTEM",
                null,
                "bi bi-megaphone", "info"
        );
    }
}

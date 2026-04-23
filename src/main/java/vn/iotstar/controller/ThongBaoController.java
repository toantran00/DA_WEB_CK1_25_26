package vn.iotstar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.ThongBao;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.service.ThongBaoService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API cho hệ thống Thông Báo (Notification).
 *
 * Client-side (STOMP/WebSocket) subscribe vào:
 *   /user/queue/notifications
 *
 * Poll fallback: GET /api/notifications
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class ThongBaoController {

    private final ThongBaoService thongBaoService;
    private final NguoiDungRepository nguoiDungRepository;

    /** GET /api/notifications — Lấy 10 thông báo mới nhất (cho dropdown) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecent() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        List<ThongBao> list = thongBaoService.getRecentNotifications(user);
        return ResponseEntity.ok(ApiResponse.success("", toDto(list)));
    }

    /** GET /api/notifications/all — Toàn bộ thông báo (trang xem tất cả) */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAll() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        return ResponseEntity.ok(ApiResponse.success("", toDto(thongBaoService.getAllNotifications(user))));
    }

    /** GET /api/notifications/unread-count — Đếm chưa đọc (cho badge đỏ) */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.ok(ApiResponse.success("", 0L));

        long count = thongBaoService.countUnread(user);
        return ResponseEntity.ok(ApiResponse.success("", count));
    }

    /** PUT /api/notifications/read-all — Đánh dấu tất cả đã đọc */
    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markAllRead() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        thongBaoService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả là đã đọc", null));
    }

    /** PUT /api/notifications/{id}/read — Đánh dấu một thông báo đã đọc */
    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markOneRead(@PathVariable Integer id) {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        thongBaoService.markAsRead(id, user);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", null));
    }

    /** DELETE /api/notifications/clear-read — Xoá thông báo đã đọc */
    @DeleteMapping("/clear-read")
    @Transactional
    public ResponseEntity<ApiResponse<String>> clearRead() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        thongBaoService.deleteReadNotifications(user);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thông báo đã đọc", null));
    }

    // ── Helper ───────────────────────────────────────────────────────

    private List<Map<String, Object>> toDto(List<ThongBao> list) {
        return list.stream().map(n -> Map.<String, Object>of(
            "maThongBao",  n.getMaThongBao(),
            "tieuDe",      n.getTieuDe(),
            "noiDung",     n.getNoiDung(),
            "loai",        n.getLoai(),
            "duongDan",    n.getDuongDan() != null ? n.getDuongDan() : "#",
            "icon",        n.getIcon(),
            "mauSac",      n.getMauSac(),
            "daDoc",       n.getDaDoc(),
            "thoiGian",    n.getThoiGianTao().toString()
        )).collect(Collectors.toList());
    }

    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return nguoiDungRepository.findByEmail(auth.getName()).orElse(null);
    }
}

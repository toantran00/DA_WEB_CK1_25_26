package vn.iotstar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.DanhSachYeuThich;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.service.DanhSachYeuThichService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API cho chức năng Danh Sách Yêu Thích (Wishlist).
 *
 * Tất cả endpoint đều yêu cầu đăng nhập (xem SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final DanhSachYeuThichService wishlistService;
    private final NguoiDungRepository nguoiDungRepository;

    /** GET /api/wishlist — Lấy toàn bộ wishlist của user hiện tại */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWishlist() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        List<DanhSachYeuThich> items = wishlistService.getWishlistByUser(user);

        List<Map<String, Object>> data = items.stream().map(item -> {
            var sp = item.getSanPham();
            return Map.<String, Object>of(
                "maYeuThich",   item.getMaYeuThich(),
                "maSanPham",    sp.getMaSanPham(),
                "tenSanPham",   sp.getTenSanPham(),
                "giaBan",       sp.getGiaBan(),
                "hinhAnh",      sp.getHinhAnh() != null ? sp.getHinhAnh() : "",
                "loaiSanPham",  sp.getLoaiSanPham() != null ? sp.getLoaiSanPham() : "",
                "soLuongConLai",sp.getSoLuongConLai(),
                "ngayThem",     item.getNgayThem().toString()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Danh sách yêu thích", data));
    }

    /**
     * POST /api/wishlist/toggle/{maSanPham}
     * Nếu chưa có → thêm; nếu có rồi → xoá. Trả về action: "ADDED" | "REMOVED"
     */
    @PostMapping("/toggle/{maSanPham}")
    public ResponseEntity<ApiResponse<String>> toggle(@PathVariable Integer maSanPham) {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        ApiResponse<String> result = wishlistService.toggleWishlist(user, maSanPham);
        return ResponseEntity.ok(result);
    }

    /** POST /api/wishlist/add/{maSanPham} — Thêm vào wishlist */
    @PostMapping("/add/{maSanPham}")
    public ResponseEntity<ApiResponse<String>> add(@PathVariable Integer maSanPham) {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        return ResponseEntity.ok(wishlistService.addToWishlist(user, maSanPham));
    }

    /** DELETE /api/wishlist/remove/{maSanPham} — Xoá khỏi wishlist */
    @DeleteMapping("/remove/{maSanPham}")
    public ResponseEntity<ApiResponse<String>> remove(@PathVariable Integer maSanPham) {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        return ResponseEntity.ok(wishlistService.removeFromWishlist(user, maSanPham));
    }

    /** GET /api/wishlist/check/{maSanPham} — Kiểm tra sản phẩm có trong wishlist không */
    @GetMapping("/check/{maSanPham}")
    public ResponseEntity<ApiResponse<Boolean>> check(@PathVariable Integer maSanPham) {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.ok(ApiResponse.success("", false));

        boolean inWishlist = wishlistService.isInWishlist(user, maSanPham);
        return ResponseEntity.ok(ApiResponse.success("", inWishlist));
    }

    /** GET /api/wishlist/count — Đếm số sản phẩm trong wishlist */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> count() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.ok(ApiResponse.success("", 0L));

        return ResponseEntity.ok(ApiResponse.success("", wishlistService.countWishlist(user)));
    }

    /** DELETE /api/wishlist/clear — Xoá toàn bộ wishlist */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clear() {
        NguoiDung user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        wishlistService.clearWishlist(user);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ danh sách yêu thích", null));
    }

    // ── Helper ───────────────────────────────────────────────────────

    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return nguoiDungRepository.findByEmail(auth.getName()).orElse(null);
    }
}

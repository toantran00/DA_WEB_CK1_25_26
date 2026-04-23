package vn.iotstar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.DanhSachYeuThich;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.DanhSachYeuThichRepository;
import vn.iotstar.repository.SanPhamRepository;
import vn.iotstar.service.DanhSachYeuThichService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanhSachYeuThichServiceImpl implements DanhSachYeuThichService {

    private final DanhSachYeuThichRepository wishlistRepository;
    private final SanPhamRepository sanPhamRepository;

    @Override
    public List<DanhSachYeuThich> getWishlistByUser(NguoiDung nguoiDung) {
        return wishlistRepository.findByNguoiDungOrderByNgayThemDesc(nguoiDung);
    }

    @Override
    @Transactional
    public ApiResponse<String> toggleWishlist(NguoiDung nguoiDung, Integer maSanPham) {
        if (isInWishlist(nguoiDung, maSanPham)) {
            return removeFromWishlist(nguoiDung, maSanPham);
        } else {
            return addToWishlist(nguoiDung, maSanPham);
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> addToWishlist(NguoiDung nguoiDung, Integer maSanPham) {
        try {
            SanPham sanPham = sanPhamRepository.findById(maSanPham)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            if (!Boolean.TRUE.equals(sanPham.getTrangThai())) {
                return ApiResponse.error("Sản phẩm không còn hoạt động");
            }

            if (wishlistRepository.existsByNguoiDungAndSanPham(nguoiDung, sanPham)) {
                return ApiResponse.error("Sản phẩm đã có trong danh sách yêu thích");
            }

            DanhSachYeuThich item = DanhSachYeuThich.builder()
                    .nguoiDung(nguoiDung)
                    .sanPham(sanPham)
                    .build();
            wishlistRepository.save(item);

            log.info("User {} added product {} to wishlist", nguoiDung.getEmail(), maSanPham);
            return ApiResponse.success("Đã thêm vào danh sách yêu thích ❤️", "ADDED");

        } catch (RuntimeException e) {
            log.error("Error adding to wishlist: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> removeFromWishlist(NguoiDung nguoiDung, Integer maSanPham) {
        try {
            SanPham sanPham = sanPhamRepository.findById(maSanPham)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Optional<DanhSachYeuThich> item = wishlistRepository.findByNguoiDungAndSanPham(nguoiDung, sanPham);
            if (item.isEmpty()) {
                return ApiResponse.error("Sản phẩm không có trong danh sách yêu thích");
            }

            wishlistRepository.delete(item.get());

            log.info("User {} removed product {} from wishlist", nguoiDung.getEmail(), maSanPham);
            return ApiResponse.success("Đã xóa khỏi danh sách yêu thích", "REMOVED");

        } catch (RuntimeException e) {
            log.error("Error removing from wishlist: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public boolean isInWishlist(NguoiDung nguoiDung, Integer maSanPham) {
        return sanPhamRepository.findById(maSanPham)
                .map(sp -> wishlistRepository.existsByNguoiDungAndSanPham(nguoiDung, sp))
                .orElse(false);
    }

    @Override
    public long countWishlist(NguoiDung nguoiDung) {
        return wishlistRepository.countByNguoiDung(nguoiDung);
    }

    @Override
    @Transactional
    public void clearWishlist(NguoiDung nguoiDung) {
        List<DanhSachYeuThich> items = wishlistRepository.findByNguoiDungOrderByNgayThemDesc(nguoiDung);
        wishlistRepository.deleteAll(items);
        log.info("Cleared wishlist for user {}", nguoiDung.getEmail());
    }
}

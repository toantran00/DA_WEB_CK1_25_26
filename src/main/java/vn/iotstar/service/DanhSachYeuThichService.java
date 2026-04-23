package vn.iotstar.service;

import vn.iotstar.entity.DanhSachYeuThich;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.model.ApiResponse;

import java.util.List;

/**
 * Service cho chức năng Danh Sách Yêu Thích (Wishlist).
 */
public interface DanhSachYeuThichService {

    /** Lấy toàn bộ wishlist của user */
    List<DanhSachYeuThich> getWishlistByUser(NguoiDung nguoiDung);

    /** Toggle: nếu chưa có → thêm vào, nếu có rồi → xoá khỏi wishlist */
    ApiResponse<String> toggleWishlist(NguoiDung nguoiDung, Integer maSanPham);

    /** Thêm sản phẩm vào wishlist */
    ApiResponse<String> addToWishlist(NguoiDung nguoiDung, Integer maSanPham);

    /** Xoá sản phẩm khỏi wishlist */
    ApiResponse<String> removeFromWishlist(NguoiDung nguoiDung, Integer maSanPham);

    /** Kiểm tra sản phẩm có trong wishlist của user không */
    boolean isInWishlist(NguoiDung nguoiDung, Integer maSanPham);

    /** Đếm số sản phẩm trong wishlist của user */
    long countWishlist(NguoiDung nguoiDung);

    /** Xoá toàn bộ wishlist của user */
    void clearWishlist(NguoiDung nguoiDung);
}

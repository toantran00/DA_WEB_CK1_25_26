package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.DanhSachYeuThich;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhSachYeuThichRepository extends JpaRepository<DanhSachYeuThich, Integer> {

    /** Lấy toàn bộ wishlist của user, sắp xếp mới nhất lên đầu */
    List<DanhSachYeuThich> findByNguoiDungOrderByNgayThemDesc(NguoiDung nguoiDung);

    /** Kiểm tra user đã thêm sản phẩm vào wishlist chưa */
    boolean existsByNguoiDungAndSanPham(NguoiDung nguoiDung, SanPham sanPham);

    /** Lấy record cụ thể để xoá */
    Optional<DanhSachYeuThich> findByNguoiDungAndSanPham(NguoiDung nguoiDung, SanPham sanPham);

    /** Xoá theo user + sản phẩm */
    @Modifying
    @Query("DELETE FROM DanhSachYeuThich d WHERE d.nguoiDung = :nguoiDung AND d.sanPham = :sanPham")
    void deleteByNguoiDungAndSanPham(@Param("nguoiDung") NguoiDung nguoiDung,
                                     @Param("sanPham") SanPham sanPham);

    /** Đếm số sản phẩm trong wishlist của user */
    long countByNguoiDung(NguoiDung nguoiDung);

    /** Đếm số user đã wishlist một sản phẩm (có thể dùng để sort by popularity) */
    long countBySanPham(SanPham sanPham);
}

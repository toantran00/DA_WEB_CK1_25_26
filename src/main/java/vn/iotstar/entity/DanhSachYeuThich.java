package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng lưu danh sách yêu thích (Wishlist) của người dùng.
 * Composite key: (MaNguoiDung, MaSanPham) — mỗi user chỉ có thể thêm
 * một sản phẩm vào wishlist một lần.
 */
@Entity
@Table(name = "DanhSachYeuThich",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"MaNguoiDung", "MaSanPham"},
           name = "UK_WishList_User_Product"
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhSachYeuThich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaYeuThich")
    private Integer maYeuThich;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    /** Thời điểm user thêm sản phẩm vào wishlist */
    @Column(name = "NgayThem", nullable = false)
    @Builder.Default
    private LocalDateTime ngayThem = LocalDateTime.now();
}

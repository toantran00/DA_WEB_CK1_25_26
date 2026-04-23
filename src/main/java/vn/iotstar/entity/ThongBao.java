package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng lưu thông báo hệ thống cho người dùng.
 * Push realtime qua WebSocket, persist để user xem lại sau.
 *
 * Loại thông báo (loai):
 *  - ORDER_STATUS   : Đơn hàng thay đổi trạng thái
 *  - NEW_ORDER      : Vendor nhận đơn hàng mới
 *  - PROMOTION      : Khuyến mãi mới từ cửa hàng user đã theo dõi
 *  - SYSTEM         : Thông báo hệ thống từ Admin
 */
@Entity
@Table(name = "ThongBao",
       indexes = {
           @Index(name = "IDX_ThongBao_NguoiNhan", columnList = "MaNguoiNhan"),
           @Index(name = "IDX_ThongBao_DaDoc", columnList = "DaDoc")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThongBao")
    private Integer maThongBao;

    /** Người nhận thông báo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNguoiNhan", nullable = false)
    private NguoiDung nguoiNhan;

    /** Tiêu đề ngắn hiển thị trên notification bell */
    @Column(name = "TieuDe", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tieuDe;

    /** Nội dung chi tiết */
    @Column(name = "NoiDung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    /**
     * Loại thông báo: ORDER_STATUS | NEW_ORDER | PROMOTION | SYSTEM
     */
    @Column(name = "Loai", nullable = false, columnDefinition = "NVARCHAR(50)")
    @Builder.Default
    private String loai = "SYSTEM";

    /** URL điều hướng khi user click vào thông báo (nullable) */
    @Column(name = "DuongDan", columnDefinition = "NVARCHAR(500)")
    private String duongDan;

    /** Icon CSS class (e.g. "bi bi-bag-check", "bi bi-star") */
    @Column(name = "Icon", columnDefinition = "NVARCHAR(100)")
    @Builder.Default
    private String icon = "bi bi-bell";

    /** Màu badge: success | warning | danger | info | primary */
    @Column(name = "MauSac", columnDefinition = "NVARCHAR(20)")
    @Builder.Default
    private String mauSac = "info";

    @Column(name = "DaDoc", nullable = false)
    @Builder.Default
    private Boolean daDoc = false;

    @Column(name = "ThoiGianTao", nullable = false)
    @Builder.Default
    private LocalDateTime thoiGianTao = LocalDateTime.now();
}

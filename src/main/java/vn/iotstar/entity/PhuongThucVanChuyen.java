package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PhuongThucVanChuyen")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhuongThucVanChuyen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhuongThuc")
    private Integer maPhuongThuc;
    
    @NotBlank(message = "Tên nhà vận chuyển không được để trống")
    @Size(max = 200, message = "Tên nhà vận chuyển không được quá 200 ký tự")
    @Column(name = "TenNhaVanChuyen", columnDefinition = "NVARCHAR(200)", nullable = false)
    private String tenNhaVanChuyen;
    
    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    @Column(name = "MoTa", columnDefinition = "NVARCHAR(500)")
    private String moTa;
    
    @NotNull(message = "Phí vận chuyển không được để trống")
    @DecimalMin(value = "0.0", message = "Phí vận chuyển không được âm")
    @Column(name = "PhiVanChuyen", precision = 18, scale = 2, nullable = false)
    private BigDecimal phiVanChuyen;
    
    @Column(name = "ThoiGianGiaoHangDuKien", columnDefinition = "NVARCHAR(100)")
    private String thoiGianGiaoHangDuKien; // VD: "2-3 ngày", "Giao hỏa tốc trong 24h"
    
    @Column(name = "TrangThai")
    @Builder.Default
    private Boolean trangThai = true; // true = Hoạt động, false = Ngưng hoạt động
    
    @Column(name = "ThuTu")
    @Builder.Default
    private Integer thuTu = 0; // Để sắp xếp thứ tự hiển thị
}

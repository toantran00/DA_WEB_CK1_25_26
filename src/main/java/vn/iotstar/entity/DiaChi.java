package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "DiaChi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDiaChi")
    private Integer maDiaChi;
    
    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    private NguoiDung nguoiDung;
    
    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận không được quá 100 ký tự")
    @Column(name = "TenNguoiNhan", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String tenNguoiNhan;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số")
    @Column(name = "SoDienThoai", nullable = false, columnDefinition = "VARCHAR(20)")
    private String soDienThoai;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được quá 500 ký tự")
    @Column(name = "DiaChiChiTiet", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String diaChiChiTiet;
    
    @Column(name = "Latitude")
    private Double latitude;
    
    @Column(name = "Longitude")
    private Double longitude;
    
    @Column(name = "MacDinh")
    @Builder.Default
    private Boolean macDinh = false;
    
    @Column(name = "TrangThai", columnDefinition = "NVARCHAR(20)")
    @Builder.Default
    private String trangThai = "Hoạt động";
}

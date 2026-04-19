package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChiDTO {
    
    private Integer maDiaChi;
    
    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận không được quá 100 ký tự")
    private String tenNguoiNhan;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số")
    private String soDienThoai;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được quá 500 ký tự")
    private String diaChiChiTiet;
    
    private Double latitude;
    
    private Double longitude;
    
    private Boolean macDinh;
    
    private String trangThai;
}

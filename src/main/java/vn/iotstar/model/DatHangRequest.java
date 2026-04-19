package vn.iotstar.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DatHangRequest {
    private Map<Integer, Integer> selectedPromotions; // Map<MaCuaHang, MaKhuyenMai>
    private String ghiChu;
    private Integer maDiaChi; // ID của địa chỉ được chọn
    private String diaChiGiaoHang; // Fallback nếu không có địa chỉ nào trong danh sách
    private String soDienThoai;
    private String phuongThucThanhToan;
    private Integer maPhuongThuc; // ID của phương thức vận chuyển được chọn
}
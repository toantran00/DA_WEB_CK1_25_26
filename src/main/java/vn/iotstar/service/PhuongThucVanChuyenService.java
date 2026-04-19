package vn.iotstar.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.iotstar.entity.PhuongThucVanChuyen;
import java.util.List;
import java.util.Optional;

public interface PhuongThucVanChuyenService {
    
    // Lấy tất cả phương thức vận chuyển
    List<PhuongThucVanChuyen> findAll();
    
    // Lấy tất cả với phân trang
    Page<PhuongThucVanChuyen> findAll(Pageable pageable);
    
    // Tìm kiếm theo tên nhà vận chuyển với phân trang
    Page<PhuongThucVanChuyen> searchByTenNhaVanChuyen(String keyword, Pageable pageable);
    
    // Lấy phương thức vận chuyển đang hoạt động
    List<PhuongThucVanChuyen> findActiveShippingMethods();
    
    // Lấy theo ID
    Optional<PhuongThucVanChuyen> findById(Integer id);
    
    // Tạo mới
    PhuongThucVanChuyen save(PhuongThucVanChuyen phuongThucVanChuyen);
    
    // Cập nhật
    PhuongThucVanChuyen update(Integer id, PhuongThucVanChuyen phuongThucVanChuyen);
    
    // Xóa
    void delete(Integer id);
    
    // Thay đổi trạng thái
    PhuongThucVanChuyen toggleStatus(Integer id);
    
    // Đếm số phương thức đang hoạt động
    long countActiveShippingMethods();
}

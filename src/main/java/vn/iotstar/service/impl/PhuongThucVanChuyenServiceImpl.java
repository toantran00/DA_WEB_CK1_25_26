package vn.iotstar.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.PhuongThucVanChuyen;
import vn.iotstar.repository.PhuongThucVanChuyenRepository;
import vn.iotstar.service.PhuongThucVanChuyenService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PhuongThucVanChuyenServiceImpl implements PhuongThucVanChuyenService {

    private final PhuongThucVanChuyenRepository phuongThucVanChuyenRepository;

    @Override
    public List<PhuongThucVanChuyen> findAll() {
        return phuongThucVanChuyenRepository.findAllByOrderByThuTuAsc();
    }

    @Override
    public Page<PhuongThucVanChuyen> findAll(Pageable pageable) {
        return phuongThucVanChuyenRepository.findAll(pageable);
    }

    @Override
    public Page<PhuongThucVanChuyen> searchByTenNhaVanChuyen(String keyword, Pageable pageable) {
        return phuongThucVanChuyenRepository.findByTenNhaVanChuyenContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public List<PhuongThucVanChuyen> findActiveShippingMethods() {
        return phuongThucVanChuyenRepository.findByTrangThaiTrueOrderByThuTuAsc();
    }

    @Override
    public Optional<PhuongThucVanChuyen> findById(Integer id) {
        return phuongThucVanChuyenRepository.findById(id);
    }

    @Override
    @Transactional
    public PhuongThucVanChuyen save(PhuongThucVanChuyen phuongThucVanChuyen) {
        return phuongThucVanChuyenRepository.save(phuongThucVanChuyen);
    }

    @Override
    @Transactional
    public PhuongThucVanChuyen update(Integer id, PhuongThucVanChuyen phuongThucVanChuyen) {
        PhuongThucVanChuyen existing = phuongThucVanChuyenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức vận chuyển với ID: " + id));
        
        existing.setTenNhaVanChuyen(phuongThucVanChuyen.getTenNhaVanChuyen());
        existing.setMoTa(phuongThucVanChuyen.getMoTa());
        existing.setPhiVanChuyen(phuongThucVanChuyen.getPhiVanChuyen());
        existing.setThoiGianGiaoHangDuKien(phuongThucVanChuyen.getThoiGianGiaoHangDuKien());
        existing.setTrangThai(phuongThucVanChuyen.getTrangThai());
        existing.setThuTu(phuongThucVanChuyen.getThuTu());
        
        return phuongThucVanChuyenRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        phuongThucVanChuyenRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PhuongThucVanChuyen toggleStatus(Integer id) {
        PhuongThucVanChuyen phuongThuc = phuongThucVanChuyenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức vận chuyển với ID: " + id));
        
        phuongThuc.setTrangThai(!phuongThuc.getTrangThai());
        return phuongThucVanChuyenRepository.save(phuongThuc);
    }

    @Override
    public long countActiveShippingMethods() {
        return phuongThucVanChuyenRepository.countByTrangThaiTrue();
    }
}

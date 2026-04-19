package vn.iotstar.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.repository.CuaHangRepository;
import vn.iotstar.repository.DanhMucRepository;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.repository.SanPhamRepository;
import vn.iotstar.repository.VaiTroRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private CuaHangRepository cuaHangRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("====== BẮT ĐẦU KHỞI TẠO DỮ LIỆU MẪU (MOCK DATA) ======");

        // 1. KHỞI TẠO VAI TRÒ
        taoVaiTroNeuChuaCo("USER", "Người dùng mặc định");
        taoVaiTroNeuChuaCo("ADMIN", "Quản trị viên hệ thống");
        taoVaiTroNeuChuaCo("VENDOR", "Chủ cửa hàng");
        taoVaiTroNeuChuaCo("SHIPPER", "Người giao hàng");

        // 2. KHỞI TẠO USER VENDOR
        NguoiDung vendor = taoNguoiDungNeuChuaCo("vendor@gmail.com", "Chủ Shop Demo", "0987654321", "VENDOR");

        // 3. KHỞI TẠO CỬA HÀNG
        CuaHang cuaHang = taoCuaHangNeuChuaCo(vendor, "PetShop Official Demo");

        // 4. KHỞI TẠO DANH MỤC
        // Sử dụng ảnh có sẵn trong thư mục uploads/categories/
        DanhMuc danhMuc1 = taoDanhMucNeuChuaCo("Thức ăn thú cưng", "Thức ăn chất lượng cao cho chó mèo",
                "/uploads/categories/category_1760881035917.jpg");
        DanhMuc danhMuc2 = taoDanhMucNeuChuaCo("Phụ kiện đồ chơi", "Vòng cổ, chuồng, đồ chơi xả stress",
                "/uploads/categories/category_1760881045546.jpg");

        // 5. KHỞI TẠO SẢN PHẨM MẪU
        if (sanPhamRepository.count() == 0 && cuaHang != null) {
            // Sử dụng hình ảnh thực tế đang có sẵn trong uploads/products/
            taoSanPham(cuaHang, danhMuc1, "Hạt Royal Canin cho Mèo",
                    "Thức ăn hạt cao cấp, giàu dinh dưỡng cho mèo phát triển", new BigDecimal("350000"), 50, "Mèo",
                    "/uploads/products/product_1760881135339.jpg");
            taoSanPham(cuaHang, danhMuc1, "Pate Whiskas Vị Cá Cơm", "100% Thịt cá hồi nguyên chất",
                    new BigDecimal("25000"), 100, "Mèo", "/uploads/products/product_1760881150537.jpg");
            taoSanPham(cuaHang, danhMuc1, "Xương Gặm Sạch Răng Cho Chó",
                    "Xương canxi giúp sạch răng, thơm miệng chó lớn", new BigDecimal("80000"), 30, "Chó",
                    "/uploads/products/product_1760881158593.jpg");

            taoSanPham(cuaHang, danhMuc2, "Đồ Chơi Cần Câu Mèo", "Giúp mèo tập thể dục và giảm xả stress cực tốt",
                    new BigDecimal("45000"), 80, "Mèo", "/uploads/products/product_1760881189774.jpg");
            taoSanPham(cuaHang, danhMuc2, "Vòng Cổ Có Chuông Cho Chó Nhỏ", "Chống đi lạc, phát âm thanh dễ nhận biết",
                    new BigDecimal("50000"), 150, "Cả hai", "/uploads/products/product_1760881198475.jpg");
            System.out.println("=> Đã tự động khởi tạo 5 SẢN PHẨM mẫu.");
        }

        System.out.println("====== KẾT THÚC KHỞI TẠO DỮ LIỆU ======");
    }

    private void taoVaiTroNeuChuaCo(String maVaiTro, String tenVaiTro) {
        if (!vaiTroRepository.existsById(maVaiTro)) {
            VaiTro vt = new VaiTro();
            vt.setMaVaiTro(maVaiTro);
            vt.setTenVaiTro(tenVaiTro);
            vaiTroRepository.save(vt);
            System.out.println("=> Khởi tạo Vai Trò: " + maVaiTro);
        }
    }

    private NguoiDung taoNguoiDungNeuChuaCo(String email, String tenNguoiDung, String sdt, String roleId) {
        Optional<NguoiDung> opt = nguoiDungRepository.findByEmail(email);
        if (opt.isPresent())
            return opt.get();

        VaiTro role = vaiTroRepository.findById(roleId).orElse(null);
        if (role == null)
            return null;

        NguoiDung user = new NguoiDung();
        user.setEmail(email);
        user.setTenNguoiDung(tenNguoiDung);
        user.setMatKhau(passwordEncoder.encode("123456"));
        user.setSdt(sdt);
        user.setVaiTro(role);
        user.setTrangThai("Hoạt động");

        System.out.println("=> Khởi tạo NguoiDung mẫu: " + email);
        return nguoiDungRepository.save(user);
    }

    private CuaHang taoCuaHangNeuChuaCo(NguoiDung vendor, String tenCuaHang) {
        if (vendor == null)
            return null;
        if (cuaHangRepository.count() > 0) {
            return cuaHangRepository.findAll().get(0);
        }

        CuaHang ch = new CuaHang();
        ch.setNguoiDung(vendor);
        ch.setTenCuaHang(tenCuaHang);
        ch.setDiaChi("123 Phố Thú Cưng, HCM");
        ch.setSoDienThoai("0987111222");
        ch.setEmail("petshopdemo@gmail.com");
        ch.setTrangThai(true);
        ch.setNgayTao(new Date());

        System.out.println("=> Khởi tạo CuaHang mẫu: " + tenCuaHang);
        return cuaHangRepository.save(ch);
    }

    private DanhMuc taoDanhMucNeuChuaCo(String tenDanhMuc, String moTa, String imageUrl) {
        // Tìm nhanh qua danh sách
        for (DanhMuc dm : danhMucRepository.findAll()) {
            if (dm.getTenDanhMuc().equals(tenDanhMuc))
                return dm;
        }

        DanhMuc dm = new DanhMuc();
        dm.setTenDanhMuc(tenDanhMuc);
        dm.setMoTa(moTa);
        dm.setHinhAnh(imageUrl);
        dm.setTrangThai(true);
        dm.setNgayTao(LocalDateTime.now());

        System.out.println("=> Khởi tạo DanhMuc mẫu: " + tenDanhMuc);
        return danhMucRepository.save(dm);
    }

    private void taoSanPham(CuaHang cuaHang, DanhMuc danhMuc, String tenSanPham, String moTa, BigDecimal giaBan,
            int soLuong, String loaiSanPham, String imageUrl) {
        SanPham sp = new SanPham();
        sp.setCuaHang(cuaHang);
        sp.setDanhMuc(danhMuc);
        sp.setTenSanPham(tenSanPham);
        sp.setMoTaSanPham(moTa);
        sp.setGiaBan(giaBan);
        sp.setSoLuongConLai(soLuong);
        sp.setSoLuongDaBan(0);
        sp.setNgayNhap(new Date());
        sp.setLoaiSanPham(loaiSanPham);
        sp.setHinhAnh(imageUrl);
        sp.setLuotThich(BigDecimal.ZERO);
        sp.setTrangThai(true);

        sanPhamRepository.save(sp);
    }
}

package vn.iotstar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.iotstar.entity.*;
import vn.iotstar.repository.DatHangChiTietRepository;
import vn.iotstar.repository.DatHangRepository;
import vn.iotstar.service.impl.DatHangServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatHangServiceImpl (Order Service).
 * Covers: findBy, save with cancellation sync, updateOrderStatus state transitions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatHangServiceImpl Unit Tests")
class DatHangServiceImplTest {

    @Mock private DatHangRepository datHangRepository;
    @Mock private DatHangChiTietRepository datHangChiTietRepository;
    @Mock private GioHangService gioHangService;
    @Mock private MatHangService matHangService;
    @Mock private CuaHangService cuaHangService;
    @Mock private KhuyenMaiService khuyenMaiService;
    @Mock private SanPhamService sanPhamService;
    @Mock private ThanhToanService thanhToanService;
    @Mock private VanChuyenService vanChuyenService;
    @Mock private PhuongThucVanChuyenService phuongThucVanChuyenService;

    @InjectMocks
    private DatHangServiceImpl datHangService;

    private DatHang mockDatHang;
    private NguoiDung mockUser;
    private CuaHang mockCuaHang;

    @BeforeEach
    void setUp() {
        mockUser = NguoiDung.builder()
                .maNguoiDung(1)
                .tenNguoiDung("Nguyen Van A")
                .email("user@example.com")
                .build();

        mockCuaHang = CuaHang.builder()
                .maCuaHang(1)
                .tenCuaHang("Pet Shop ABC")
                .trangThai(true)
                .build();

        mockDatHang = DatHang.builder()
                .maDatHang(100)
                .nguoiDung(mockUser)
                .cuaHang(mockCuaHang)
                .ngayDat(LocalDate.now())
                .tongTien(new BigDecimal("250000"))
                .trangThai("Chờ xác nhận")
                .diaChiGiaoHang("123 Đường ABC, TP.HCM")
                .soDienThoaiGiaoHang("0901234567")
                .phiVanChuyen(BigDecimal.ZERO)
                .build();
    }

    // ===== findByMaDatHang tests =====

    @Test
    @DisplayName("findByMaDatHang - existing order returns order")
    void findByMaDatHang_exists_returnsOrder() {
        when(datHangRepository.findById(100)).thenReturn(Optional.of(mockDatHang));

        DatHang result = datHangService.findByMaDatHang(100);

        assertThat(result).isNotNull();
        assertThat(result.getMaDatHang()).isEqualTo(100);
        assertThat(result.getTrangThai()).isEqualTo("Chờ xác nhận");
    }

    @Test
    @DisplayName("findByMaDatHang - non-existent order returns null")
    void findByMaDatHang_notExists_returnsNull() {
        when(datHangRepository.findById(999)).thenReturn(Optional.empty());

        DatHang result = datHangService.findByMaDatHang(999);

        assertThat(result).isNull();
    }

    // ===== save tests =====

    @Test
    @DisplayName("save - new order is saved successfully")
    void save_newOrder_savedSuccessfully() {
        when(datHangRepository.save(any(DatHang.class))).thenReturn(mockDatHang);

        DatHang saved = datHangService.save(mockDatHang);

        assertThat(saved).isNotNull();
        assertThat(saved.getMaDatHang()).isEqualTo(100);
        verify(datHangRepository, atLeastOnce()).save(mockDatHang);
    }

    @Test
    @DisplayName("save - cancelling order triggers delivery sync")
    void save_cancellingOrder_triggersDeliverySync() {
        // Arrange: existing order in "Chờ xác nhận" state
        DatHang existingOrder = DatHang.builder()
                .maDatHang(100)
                .trangThai("Chờ xác nhận")
                .build();

        DatHang cancelledOrder = DatHang.builder()
                .maDatHang(100)
                .trangThai("Hủy")
                .lyDoHuy("Khách hàng yêu cầu hủy")
                .build();

        when(datHangRepository.findById(100)).thenReturn(Optional.of(existingOrder));
        when(datHangRepository.save(cancelledOrder)).thenReturn(cancelledOrder);

        // VanChuyen already cancelled or not found — just verify no exception thrown
        when(vanChuyenService.getDeliveryByOrderId(100)).thenReturn(null);

        // Act
        DatHang result = datHangService.save(cancelledOrder);

        // Assert: no exception, delivery sync was attempted
        assertThat(result.getTrangThai()).isEqualTo("Hủy");
        verify(vanChuyenService, times(1)).getDeliveryByOrderId(100);
    }

    @Test
    @DisplayName("save - order not previously cancelled, no sync triggered if status unchanged")
    void save_noStatusChange_noSync() {
        // Arrange: existing order already in "Chờ xác nhận", new order also "Chờ xác nhận"
        DatHang unchanged = DatHang.builder()
                .maDatHang(100)
                .trangThai("Chờ xác nhận")
                .build();

        when(datHangRepository.findById(100)).thenReturn(Optional.of(unchanged));
        when(datHangRepository.save(unchanged)).thenReturn(unchanged);

        datHangService.save(unchanged);

        // VanChuyenService should NOT be called since status didn't change to "Hủy"
        verifyNoInteractions(vanChuyenService);
    }

    // ===== countAllOrders tests =====

    @Test
    @DisplayName("countAllOrders - returns repository count")
    void countAllOrders_returnsCorrectCount() {
        when(datHangRepository.count()).thenReturn(42L);

        long count = datHangService.countAllOrders();

        assertThat(count).isEqualTo(42L);
    }

    // ===== countByCuaHangAndTrangThai tests =====

    @Test
    @DisplayName("countByCuaHangAndTrangThai - returns correct count for store and status")
    void countByCuaHangAndTrangThai_returnsCount() {
        when(datHangRepository.countByCuaHangAndTrangThai(mockCuaHang, "Hoàn thành")).thenReturn(15L);

        long count = datHangService.countByCuaHangAndTrangThai(mockCuaHang, "Hoàn thành");

        assertThat(count).isEqualTo(15L);
    }

    // ===== deleteDatHang tests =====

    @Test
    @DisplayName("deleteDatHang - existing order is deleted")
    void deleteDatHang_existingOrder_deletedSuccessfully() {
        when(datHangRepository.findById(100)).thenReturn(Optional.of(mockDatHang));

        datHangService.deleteDatHang(100);

        verify(datHangRepository, times(1)).delete(mockDatHang);
    }

    @Test
    @DisplayName("deleteDatHang - non-existent order throws RuntimeException")
    void deleteDatHang_notExisting_throwsException() {
        when(datHangRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> datHangService.deleteDatHang(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không tồn tại");
    }

    // ===== getTotalRevenueByCuaHangAndDateRange tests =====

    @Test
    @DisplayName("getTotalRevenueByCuaHangAndDateRange - returns correct revenue")
    void getTotalRevenue_validRange_returnsRevenue() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        when(datHangChiTietRepository.getTotalRevenueByCuaHangAndDateRange(
                mockCuaHang, "Hoàn thành", start, end)).thenReturn(5_000_000.0);

        Double revenue = datHangService.getTotalRevenueByCuaHangAndDateRange(
                mockCuaHang, "Hoàn thành", start, end);

        assertThat(revenue).isEqualTo(5_000_000.0);
    }
}

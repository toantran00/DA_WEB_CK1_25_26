package vn.iotstar.controller.vendor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vendor Chart API — JSON data cho Chart.js trên Vendor Dashboard.
 *
 * Endpoints:
 *  GET /vendor/api/charts/revenue-by-month    → Doanh thu 12 tháng của cửa hàng
 *  GET /vendor/api/charts/orders-by-status    → Tỉ lệ đơn hàng theo trạng thái
 *  GET /vendor/api/charts/top-products        → Top sản phẩm bán chạy của cửa hàng
 *  GET /vendor/api/charts/revenue-range       → Doanh thu theo khoảng ngày
 */
@Slf4j
@RestController
@RequestMapping("/vendor/api/charts")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorChartApiController {

    private final NguoiDungRepository nguoiDungRepository;
    private final CuaHangRepository cuaHangRepository;
    private final DatHangRepository datHangRepository;
    private final DatHangChiTietRepository datHangChiTietRepository;

    /**
     * Doanh thu theo tháng trong năm của cửa hàng.
     */
    @GetMapping("/revenue-by-month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenueByMonth(
            @RequestParam(defaultValue = "0") int year) {

        NguoiDung vendor = getCurrentVendor();
        if (vendor == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        CuaHang cuaHang = getCuaHang(vendor);
        if (cuaHang == null) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy cửa hàng"));

        int targetYear = year > 0 ? year : LocalDate.now().getYear();

        List<Object[]> raw = datHangChiTietRepository
                .getRevenueByMonthAndYearAndCuaHang(targetYear, "Hoàn thành", cuaHang);

        Map<Integer, Double> revenueMap = raw.stream()
                .collect(Collectors.toMap(
                    r -> ((Number) r[0]).intValue(),
                    r -> r[1] != null ? ((Number) r[1]).doubleValue() : 0.0
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data   = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            labels.add("T" + m);
            data.add(revenueMap.getOrDefault(m, 0.0));
        }

        Map<String, Object> result = Map.of(
            "labels",       labels,
            "data",         data,
            "year",         targetYear,
            "totalRevenue", data.stream().mapToDouble(Double::doubleValue).sum(),
            "tenCuaHang",   cuaHang.getTenCuaHang()
        );
        return ResponseEntity.ok(ApiResponse.success("Revenue by month", result));
    }

    /**
     * Tỉ lệ đơn hàng theo trạng thái của cửa hàng — Pie chart.
     */
    @GetMapping("/orders-by-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ordersByStatus() {
        NguoiDung vendor = getCurrentVendor();
        if (vendor == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        CuaHang cuaHang = getCuaHang(vendor);
        if (cuaHang == null) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy cửa hàng"));

        List<Object[]> raw = datHangRepository.countGroupByTrangThaiAndCuaHang(cuaHang);

        List<String> labels  = new ArrayList<>();
        List<Long>   data    = new ArrayList<>();
        List<String> colors  = new ArrayList<>();

        Map<String, String> colorMap = Map.of(
            "Chờ xác nhận", "#FFC107",
            "Xác nhận",     "#17A2B8",
            "Đang giao",    "#007BFF",
            "Hoàn thành",   "#28A745",
            "Hủy",          "#DC3545"
        );

        for (Object[] row : raw) {
            String status = (String) row[0];
            labels.add(status);
            data.add(((Number) row[1]).longValue());
            colors.add(colorMap.getOrDefault(status, "#6C757D"));
        }

        Map<String, Object> result = Map.of("labels", labels, "data", data, "colors", colors);
        return ResponseEntity.ok(ApiResponse.success("Orders by status", result));
    }

    /**
     * Top sản phẩm bán chạy của cửa hàng — Bar chart.
     */
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<Map<String, Object>>> topProducts(
            @RequestParam(defaultValue = "8") int limit) {

        NguoiDung vendor = getCurrentVendor();
        if (vendor == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        CuaHang cuaHang = getCuaHang(vendor);
        if (cuaHang == null) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy cửa hàng"));

        List<Object[]> raw = datHangChiTietRepository.getTopSellingProductsByCuaHang(cuaHang, limit);

        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();

        for (Object[] row : raw) {
            labels.add((String) row[0]);                             // tenSanPham
            data.add(((Number) row[1]).longValue());                 // soLuongBan
            revenue.add(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0); // doanhThu
        }

        Map<String, Object> result = Map.of("labels", labels, "data", data, "revenue", revenue);
        return ResponseEntity.ok(ApiResponse.success("Top products", result));
    }

    /**
     * Doanh thu theo khoảng ngày — Line chart.
     */
    @GetMapping("/revenue-range")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenueByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        NguoiDung vendor = getCurrentVendor();
        if (vendor == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        CuaHang cuaHang = getCuaHang(vendor);
        if (cuaHang == null) return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy cửa hàng"));

        List<Object[]> raw = datHangChiTietRepository
                .getRevenueByDateRangeAndCuaHang(startDate, endDate, "Hoàn thành", cuaHang);

        Map<LocalDate, Double> revenueMap = raw.stream()
                .collect(Collectors.toMap(
                    r -> ((java.sql.Date) r[0]).toLocalDate(),
                    r -> r[1] != null ? ((Number) r[1]).doubleValue() : 0.0
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data   = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            labels.add(cur.toString());
            data.add(revenueMap.getOrDefault(cur, 0.0));
            cur = cur.plusDays(1);
        }

        Map<String, Object> result = Map.of(
            "labels", labels,
            "data", data,
            "totalRevenue", data.stream().mapToDouble(Double::doubleValue).sum()
        );
        return ResponseEntity.ok(ApiResponse.success("Revenue by date range", result));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private NguoiDung getCurrentVendor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return nguoiDungRepository.findByEmail(auth.getName()).orElse(null);
    }

    private CuaHang getCuaHang(NguoiDung vendor) {
        List<CuaHang> list = cuaHangRepository.findByNguoiDung(vendor);
        return list.isEmpty() ? null : list.get(0);
    }
}

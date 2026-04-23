package vn.iotstar.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.DatHangChiTietRepository;
import vn.iotstar.repository.DatHangRepository;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.repository.SanPhamRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Chart API — cung cấp dữ liệu JSON cho Chart.js trên trang Dashboard.
 *
 * Endpoints:
 *  GET /admin/api/charts/revenue-by-month   → Line chart doanh thu 12 tháng
 *  GET /admin/api/charts/orders-by-status   → Pie chart tỉ lệ trạng thái đơn hàng
 *  GET /admin/api/charts/top-products       → Bar chart top 10 sản phẩm bán chạy
 *  GET /admin/api/charts/new-users-by-month → Line chart user đăng ký theo tháng
 *  GET /admin/api/charts/revenue-range      → Doanh thu theo khoảng ngày tuỳ chọn
 */
@Slf4j
@RestController
@RequestMapping("/admin/api/charts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminChartApiController {

    private final DatHangRepository datHangRepository;
    private final DatHangChiTietRepository datHangChiTietRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;

    /**
     * Doanh thu theo tháng trong năm hiện tại.
     * Response: { labels: ["T1",...,"T12"], datasets: [{ data: [1200000,...] }] }
     */
    @GetMapping("/revenue-by-month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenueByMonth(
            @RequestParam(defaultValue = "0") int year) {

        int targetYear = year > 0 ? year : LocalDate.now().getYear();

        // Query: (tháng, tổng doanh thu) cho đơn Hoàn thành
        List<Object[]> raw = datHangChiTietRepository.getRevenueByMonthAndYear(targetYear, "Hoàn thành");

        // Map month -> revenue
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
            "labels", labels,
            "data",   data,
            "year",   targetYear,
            "totalRevenue", data.stream().mapToDouble(Double::doubleValue).sum()
        );
        return ResponseEntity.ok(ApiResponse.success("Revenue by month", result));
    }

    /**
     * Tỉ lệ trạng thái đơn hàng — Pie chart.
     * Response: { labels: ["Chờ xác nhận", ...], data: [12, 45, ...] }
     */
    @GetMapping("/orders-by-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ordersByStatus() {
        List<Object[]> raw = datHangRepository.countGroupByTrangThai();

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
            Long   count  = ((Number) row[1]).longValue();
            labels.add(status);
            data.add(count);
            colors.add(colorMap.getOrDefault(status, "#6C757D"));
        }

        Map<String, Object> result = Map.of(
            "labels", labels,
            "data",   data,
            "colors", colors
        );
        return ResponseEntity.ok(ApiResponse.success("Orders by status", result));
    }

    /**
     * Top 10 sản phẩm bán chạy nhất — Bar chart.
     */
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<Map<String, Object>>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {

        List<Object[]> raw = datHangChiTietRepository.getTopSellingProducts(limit);

        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();

        for (Object[] row : raw) {
            labels.add((String) row[0]);                      // tenSanPham
            data.add(((Number) row[1]).longValue());          // soLuongBan
        }

        Map<String, Object> result = Map.of("labels", labels, "data", data);
        return ResponseEntity.ok(ApiResponse.success("Top products", result));
    }

    /**
     * User đăng ký mới theo tháng — Line chart.
     */
    @GetMapping("/new-users-by-month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> newUsersByMonth(
            @RequestParam(defaultValue = "0") int year) {

        int targetYear = year > 0 ? year : LocalDate.now().getYear();
        List<Object[]> raw = nguoiDungRepository.countNewUsersByMonth(targetYear);

        Map<Integer, Long> userMap = raw.stream()
                .collect(Collectors.toMap(
                    r -> ((Number) r[0]).intValue(),
                    r -> ((Number) r[1]).longValue()
                ));

        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            labels.add("T" + m);
            data.add(userMap.getOrDefault(m, 0L));
        }

        Map<String, Object> result = Map.of("labels", labels, "data", data, "year", targetYear);
        return ResponseEntity.ok(ApiResponse.success("New users by month", result));
    }

    /**
     * Doanh thu theo khoảng ngày tuỳ chọn — Line chart theo ngày.
     */
    @GetMapping("/revenue-range")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenueByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Object[]> raw = datHangChiTietRepository.getRevenueByDateRange(startDate, endDate, "Hoàn thành");

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
}

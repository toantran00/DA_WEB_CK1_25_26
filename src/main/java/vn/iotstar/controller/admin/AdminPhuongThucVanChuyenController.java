package vn.iotstar.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.PhuongThucVanChuyen;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.PhuongThucVanChuyenService;
import vn.iotstar.service.UserDetailsImpl;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/shipping-methods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPhuongThucVanChuyenController {

    private final PhuongThucVanChuyenService phuongThucVanChuyenService;
    private final NguoiDungService nguoiDungService;

    // ============= PAGE DISPLAY METHODS =============

    @GetMapping
    public String listShippingMethods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "thuTu") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication,
            Model model) {
        
        // Lấy thông tin user đang đăng nhập
        NguoiDung user = getCurrentUser(authentication);
        model.addAttribute("user", user);
        
        // Tạo Sort object với xử lý đặc biệt
        Sort sort = createSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Lấy danh sách phương thức vận chuyển với tìm kiếm
        Page<PhuongThucVanChuyen> shippingMethodPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            shippingMethodPage = searchShippingMethods(keyword.trim(), pageable);
        } else {
            shippingMethodPage = phuongThucVanChuyenService.findAll(pageable);
        }
        
        // Thêm vào model
        model.addAttribute("shippingMethodPage", shippingMethodPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);	
        model.addAttribute("activeCount", phuongThucVanChuyenService.countActiveShippingMethods());
        model.addAttribute("totalCount", shippingMethodPage.getTotalElements());
        
        return "admin/shipping-methods/list";
    }

    // Xem chi tiết phương thức vận chuyển
    @GetMapping("/view/{id}")
    public String viewShippingMethod(@PathVariable Integer id, Model model, 
                                     RedirectAttributes redirectAttributes,
                                     Authentication authentication) {
        // Lấy thông tin user
        NguoiDung user = getCurrentUser(authentication);
        model.addAttribute("user", user);
        
        return phuongThucVanChuyenService.findById(id)
            .map(shippingMethod -> {
                model.addAttribute("shippingMethod", shippingMethod);
                return "admin/shipping-methods/view";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phương thức vận chuyển!");
                return "redirect:/admin/shipping-methods";
            });
    }

    // Hiển thị form thêm mới
    @GetMapping("/add")
    public String showAddForm(Model model, Authentication authentication) {
        NguoiDung user = getCurrentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("shippingMethod", new PhuongThucVanChuyen());
        model.addAttribute("isEdit", false);
        return "admin/shipping-methods/form";
    }

    // Xử lý thêm mới
    @PostMapping("/add")
    public String addShippingMethod(@Valid @ModelAttribute("shippingMethod") PhuongThucVanChuyen phuongThucVanChuyen,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes,
                                    Model model,
                                    Authentication authentication) {
        if (result.hasErrors()) {
            NguoiDung user = getCurrentUser(authentication);
            model.addAttribute("user", user);
            model.addAttribute("isEdit", false);
            return "admin/shipping-methods/form";
        }

        try {
            phuongThucVanChuyenService.save(phuongThucVanChuyen);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm phương thức vận chuyển thành công!");
            return "redirect:/admin/shipping-methods";
        } catch (Exception e) {
            NguoiDung user = getCurrentUser(authentication);
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/shipping-methods/form";
        }
    }

    // Hiển thị form chỉnh sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, 
                              RedirectAttributes redirectAttributes,
                              Authentication authentication) {
        NguoiDung user = getCurrentUser(authentication);
        model.addAttribute("user", user);
        
        return phuongThucVanChuyenService.findById(id)
            .map(shippingMethod -> {
                model.addAttribute("shippingMethod", shippingMethod);
                model.addAttribute("isEdit", true);
                return "admin/shipping-methods/form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phương thức vận chuyển!");
                return "redirect:/admin/shipping-methods";
            });
    }

    // Xử lý cập nhật
    @PostMapping("/edit/{id}")
    public String updateShippingMethod(@PathVariable Integer id,
                                       @Valid @ModelAttribute("shippingMethod") PhuongThucVanChuyen phuongThucVanChuyen,
                                       BindingResult result,
                                       RedirectAttributes redirectAttributes,
                                       Model model,
                                       Authentication authentication) {
        if (result.hasErrors()) {
            NguoiDung user = getCurrentUser(authentication);
            model.addAttribute("user", user);
            model.addAttribute("isEdit", true);
            return "admin/shipping-methods/form";
        }

        try {
            phuongThucVanChuyenService.update(id, phuongThucVanChuyen);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phương thức vận chuyển thành công!");
            return "redirect:/admin/shipping-methods";
        } catch (Exception e) {
            NguoiDung user = getCurrentUser(authentication);
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("isEdit", true);
            return "admin/shipping-methods/form";
        }
    }

    // Xóa phương thức vận chuyển
    @PostMapping("/delete/{id}")
    public String deleteShippingMethod(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            phuongThucVanChuyenService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phương thức vận chuyển thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/shipping-methods";
    }

    // Thay đổi trạng thái
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            phuongThucVanChuyenService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/shipping-methods";
    }

    // ============= PRIVATE HELPER METHODS =============

    private NguoiDung getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return nguoiDungService.getUserByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    private Sort createSort(String sortBy, String sortDir) {
        // Xử lý trường hợp đặc biệt: phiVanChuyen_desc
        String actualSortBy = sortBy;
        String actualSortDir = sortDir;
        
        // Kiểm tra nếu sortBy chứa "_desc" ở cuối
        if (sortBy != null && sortBy.endsWith("_desc")) {
            actualSortBy = sortBy.substring(0, sortBy.length() - 5); // Bỏ "_desc"
            actualSortDir = "desc";
        }
        
        // Đảm bảo sortBy không null hoặc empty
        if (actualSortBy == null || actualSortBy.trim().isEmpty()) {
            actualSortBy = "thuTu";
            actualSortDir = "asc";
        }
        
        // Tạo Sort object
        if ("desc".equalsIgnoreCase(actualSortDir)) {
            return Sort.by(actualSortBy).descending();
        } else {
            return Sort.by(actualSortBy).ascending();
        }
    }

    private Page<PhuongThucVanChuyen> searchShippingMethods(String keyword, Pageable pageable) {
        // Tìm kiếm theo tên nhà vận chuyển
        return phuongThucVanChuyenService.searchByTenNhaVanChuyen(keyword, pageable);
    }
}
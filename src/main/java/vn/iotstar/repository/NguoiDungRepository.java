package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.NguoiDung;

import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Integer>, JpaSpecificationExecutor<NguoiDung> {
    Optional<NguoiDung> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<NguoiDung> findTop5ByOrderByMaNguoiDungDesc();
    
    // Äáº¿m tá»•ng sá»‘ ngÆ°á»i dÃ¹ng
    long count();
    
    // TÃ¬m ngÆ°á»i dÃ¹ng theo vai trÃ²
    List<NguoiDung> findByVaiTro_MaVaiTro(String maVaiTro);
    
    // TÃ¬m ngÆ°á»i dÃ¹ng theo mÃ£ vai trÃ² vÃ  tráº¡ng thÃ¡i
    List<NguoiDung> findByVaiTro_MaVaiTroAndTrangThai(String maVaiTro, String trangThai);
    
    // TÃ¬m kiáº¿m ngÆ°á»i dÃ¹ng theo tÃªn hoáº·c email
    @Query("SELECT n FROM NguoiDung n WHERE LOWER(n.tenNguoiDung) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<NguoiDung> searchByTenNguoiDungOrEmail(String keyword);
    
    @Query("SELECT n FROM NguoiDung n WHERE LOWER(n.tenNguoiDung) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<NguoiDung> searchByTenNguoiDungOrEmail(@Param("keyword") String keyword, Pageable pageable);
    
    // 1. Äáº¿m ngÆ°á»i dÃ¹ng theo vai trÃ² (chá»‰ cáº§n VENDOR vÃ  SHIPPER)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.vaiTro.maVaiTro = :maVaiTro")
    long countByVaiTroMaVaiTro(@Param("maVaiTro") String maVaiTro);
    
 // Äáº¿m tá»•ng ngÆ°á»i dÃ¹ng thÃ´ng thÆ°á»ng (USER)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.vaiTro.maVaiTro = 'USER'")
    long countUsers();

    // Äáº¿m tá»•ng ngÆ°á»i dÃ¹ng Ä‘ang hoáº¡t Ä‘á»™ng (bao gá»“m cáº£ ADMIN)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.trangThai = 'Hoáº¡t Ä‘á»™ng'")
    long countAllActiveUsers();

    // Äáº¿m tá»•ng ngÆ°á»i dÃ¹ng bá»‹ khÃ³a (bao gá»“m cáº£ ADMIN)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.trangThai = 'KhÃ³a'")
    long countAllInactiveUsers();
    
    NguoiDung findByMaNguoiDung(Integer maNguoiDung);
    
 // TÃ¬m ngÆ°á»i dÃ¹ng theo tráº¡ng thÃ¡i
    List<NguoiDung> findByTrangThai(String trangThai);
    
    // Äáº¿m sá»‘ ngÆ°á»i dÃ¹ng theo tráº¡ng thÃ¡i
    long countByTrangThai(String trangThai);
    /** User moi dang ky theo thang trong nam (Admin Chart) */
    @Query("SELECT MONTH(n.maNguoiDung), COUNT(n) FROM NguoiDung n WHERE n.vaiTro.maVaiTro = 'USER' GROUP BY MONTH(n.maNguoiDung)")
    List<Object[]> countNewUsersByMonth(@Param("year") int year);
}
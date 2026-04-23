package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.ThongBao;

import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Integer> {

    /** Lấy tất cả thông báo của user, mới nhất trước */
    List<ThongBao> findByNguoiNhanOrderByThoiGianTaoDesc(NguoiDung nguoiNhan);

    /** Lấy N thông báo gần nhất (cho notification dropdown) */
    List<ThongBao> findTop10ByNguoiNhanOrderByThoiGianTaoDesc(NguoiDung nguoiNhan);

    /** Đếm số thông báo chưa đọc */
    long countByNguoiNhanAndDaDoc(NguoiDung nguoiNhan, Boolean daDoc);

    /** Đánh dấu tất cả là đã đọc */
    @Modifying
    @Query("UPDATE ThongBao t SET t.daDoc = true WHERE t.nguoiNhan = :nguoiNhan AND t.daDoc = false")
    int markAllAsRead(@Param("nguoiNhan") NguoiDung nguoiNhan);

    /** Đánh dấu một thông báo là đã đọc */
    @Modifying
    @Query("UPDATE ThongBao t SET t.daDoc = true WHERE t.maThongBao = :maThongBao AND t.nguoiNhan = :nguoiNhan")
    int markOneAsRead(@Param("maThongBao") Integer maThongBao,
                      @Param("nguoiNhan") NguoiDung nguoiNhan);

    /** Xoá thông báo đã đọc (dọn dẹp) */
    @Modifying
    @Query("DELETE FROM ThongBao t WHERE t.nguoiNhan = :nguoiNhan AND t.daDoc = true")
    int deleteReadNotifications(@Param("nguoiNhan") NguoiDung nguoiNhan);
}

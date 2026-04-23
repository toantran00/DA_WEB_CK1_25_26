package vn.iotstar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.model.NguoiDungModel;
import vn.iotstar.repository.CuaHangRepository;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.repository.VaiTroRepository;
import vn.iotstar.service.impl.NguoiDungServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NguoiDungServiceImpl (User Service).
 * Covers: create, update, lock/unlock, search, and status management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NguoiDungServiceImpl Unit Tests")
class NguoiDungServiceImplTest {

    @Mock private NguoiDungRepository nguoiDungRepository;
    @Mock private VaiTroRepository vaiTroRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CuaHangRepository cuaHangRepository;
    @Mock private DiaChiService diaChiService;

    @InjectMocks
    private NguoiDungServiceImpl nguoiDungService;

    private VaiTro userRole;
    private NguoiDung mockUser;

    @BeforeEach
    void setUp() {
        userRole = new VaiTro();
        userRole.setMaVaiTro("USER");
        userRole.setTenVaiTro("USER");

        mockUser = NguoiDung.builder()
                .maNguoiDung(1)
                .tenNguoiDung("Nguyen Van A")
                .email("user@example.com")
                .matKhau("$2a$10$hashed")
                .trangThai("Hoạt động")
                .vaiTro(userRole)
                .build();
    }

    // ===== createUser tests =====

    @Test
    @DisplayName("createUser - valid data creates user successfully")
    void createUser_validData_success() {
        NguoiDungModel model = new NguoiDungModel();
        model.setTenNguoiDung("Tran Van B");
        model.setEmail("newuser@example.com");
        model.setMatKhau("pass123");
        model.setMaVaiTro("USER");
        model.setTrangThai("Hoạt động");

        when(nguoiDungRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(vaiTroRepository.findById("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$10$encoded");
        when(nguoiDungRepository.save(any(NguoiDung.class))).thenReturn(mockUser);

        NguoiDung result = nguoiDungService.createUser(model);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("pass123");
        verify(nguoiDungRepository).save(any(NguoiDung.class));
    }

    @Test
    @DisplayName("createUser - duplicate email throws RuntimeException")
    void createUser_duplicateEmail_throwsException() {
        NguoiDungModel model = new NguoiDungModel();
        model.setEmail("existing@example.com");

        when(nguoiDungRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> nguoiDungService.createUser(model))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email");
        verify(nguoiDungRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - invalid role throws RuntimeException")
    void createUser_invalidRole_throwsException() {
        NguoiDungModel model = new NguoiDungModel();
        model.setEmail("user@example.com");
        model.setMaVaiTro("UNKNOWN_ROLE");

        when(nguoiDungRepository.existsByEmail(anyString())).thenReturn(false);
        when(vaiTroRepository.findById("UNKNOWN_ROLE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nguoiDungService.createUser(model))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vai trò");
    }

    // ===== getUserById tests =====

    @Test
    @DisplayName("getUserById - existing user returned")
    void getUserById_exists_returnsUser() {
        when(nguoiDungRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Optional<NguoiDung> result = nguoiDungService.getUserById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("getUserById - non-existent user returns empty")
    void getUserById_notExists_returnsEmpty() {
        when(nguoiDungRepository.findById(999)).thenReturn(Optional.empty());

        Optional<NguoiDung> result = nguoiDungService.getUserById(999);

        assertThat(result).isEmpty();
    }

    // ===== lockUser / unlockUser tests =====

    @Test
    @DisplayName("lockUser - sets status to Khóa")
    void lockUser_activeUser_setsLockedStatus() {
        when(nguoiDungRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(inv -> inv.getArgument(0));
        // mockUser has role USER (not VENDOR), so cuaHangRepository.findByNguoiDung is NOT called

        NguoiDung result = nguoiDungService.lockUser(1, "Vi phạm điều khoản");

        assertThat(result.getTrangThai()).isEqualTo("Khóa");
        assertThat(result.getLyDoKhoa()).isEqualTo("Vi phạm điều khoản");
    }

    @Test
    @DisplayName("unlockUser - sets status back to Hoạt động")
    void unlockUser_lockedUser_setsActiveStatus() {
        NguoiDung lockedUser = NguoiDung.builder()
                .maNguoiDung(1)
                .email("locked@example.com")
                .trangThai("Khóa")
                .lyDoKhoa("Vi phạm")
                .vaiTro(userRole)
                .build();

        when(nguoiDungRepository.findById(1)).thenReturn(Optional.of(lockedUser));
        when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(inv -> inv.getArgument(0));
        // userRole is USER (not VENDOR), so cuaHangRepository.findByNguoiDung is NOT called

        NguoiDung result = nguoiDungService.unlockUser(1);

        assertThat(result.getTrangThai()).isEqualTo("Hoạt động");
        assertThat(result.getLyDoKhoa()).isNull();
    }

    // ===== isUserLocked tests =====

    @Test
    @DisplayName("isUserLocked - locked user returns true")
    void isUserLocked_lockedUser_returnsTrue() {
        NguoiDung lockedUser = NguoiDung.builder().trangThai("Khóa").build();
        when(nguoiDungRepository.findById(1)).thenReturn(Optional.of(lockedUser));

        assertThat(nguoiDungService.isUserLocked(1)).isTrue();
    }

    @Test
    @DisplayName("isUserLocked - active user returns false")
    void isUserLocked_activeUser_returnsFalse() {
        when(nguoiDungRepository.findById(1)).thenReturn(Optional.of(mockUser));

        assertThat(nguoiDungService.isUserLocked(1)).isFalse();
    }

    // ===== searchUsers tests =====

    @Test
    @DisplayName("searchUsers - blank keyword returns all users")
    void searchUsers_blankKeyword_returnsAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<NguoiDung> page = new PageImpl<>(List.of(mockUser));
        when(nguoiDungRepository.findAll(pageable)).thenReturn(page);

        Page<NguoiDung> result = nguoiDungService.searchUsers("", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(nguoiDungRepository).findAll(pageable);
    }

    // ===== existsByEmail tests =====

    @Test
    @DisplayName("existsByEmail - returns true when email exists")
    void existsByEmail_exists_returnsTrue() {
        when(nguoiDungRepository.existsByEmail("user@example.com")).thenReturn(true);
        assertThat(nguoiDungService.existsByEmail("user@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - returns false when email not found")
    void existsByEmail_notFound_returnsFalse() {
        when(nguoiDungRepository.existsByEmail("ghost@example.com")).thenReturn(false);
        assertThat(nguoiDungService.existsByEmail("ghost@example.com")).isFalse();
    }

    // ===== countTotalUsers tests =====

    @Test
    @DisplayName("countTotalUsers - returns correct count from repository")
    void countTotalUsers_returnsCount() {
        when(nguoiDungRepository.count()).thenReturn(100L);
        assertThat(nguoiDungService.countTotalUsers()).isEqualTo(100L);
    }
}

package vn.iotstar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.model.LoginModel;
import vn.iotstar.model.NguoiDungModel;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.repository.VaiTroRepository;
import vn.iotstar.service.impl.AuthServiceImpl;
import vn.iotstar.util.JwtUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AuthServiceImpl.
 * Uses Mockito to isolate the service layer from database and security context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    @Mock
    private VaiTroRepository vaiTroRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private DiaChiService diaChiService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private NguoiDung mockUser;
    private VaiTro mockRole;

    @BeforeEach
    void setUp() {
        mockRole = new VaiTro();
        mockRole.setMaVaiTro("USER");
        mockRole.setTenVaiTro("USER");

        mockUser = NguoiDung.builder()
                .maNguoiDung(1)
                .tenNguoiDung("Nguyen Van A")
                .email("test@example.com")
                .matKhau("$2a$10$hashedPassword")
                .sdt("0901234567")
                .trangThai("Hoạt động")
                .vaiTro(mockRole)
                .build();
    }

    // ===== authenticateUser tests =====

    @Test
    @DisplayName("authenticateUser - success calls jwtUtil and returns successful response")
    void authenticateUser_success_returnsToken() {
        // Arrange
        LoginModel loginModel = new LoginModel();
        loginModel.setEmail("test@example.com");
        loginModel.setMatKhau("password123");

        // authenticationManager returns a specific auth object
        Authentication returnedAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(returnedAuth);
        // Use lenient() since SecurityContextHolder may alter the auth reference internally
        lenient().when(jwtUtil.generateJwtToken(any())).thenReturn("mock.jwt.token");

        // Act
        ApiResponse<String> response = authService.authenticateUser(loginModel);

        // Assert: the response must be successful and jwtUtil must have been called
        assertThat(response.isSuccess()).isTrue();
        verify(jwtUtil, times(1)).generateJwtToken(any());
    }


    @Test
    @DisplayName("authenticateUser - bad credentials returns error")
    void authenticateUser_badCredentials_returnsError() {
        // Arrange
        LoginModel loginModel = new LoginModel();
        loginModel.setEmail("test@example.com");
        loginModel.setMatKhau("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ApiResponse<String> response = authService.authenticateUser(loginModel);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("không đúng");
        verifyNoInteractions(jwtUtil);
    }

    // ===== registerUser tests =====

    @Test
    @DisplayName("registerUser - success with new email")
    void registerUser_validData_returnsSuccess() {
        // Arrange
        NguoiDungModel model = new NguoiDungModel();
        model.setTenNguoiDung("Nguyen Van B");
        model.setEmail("newuser@example.com");
        model.setMatKhau("password123");
        model.setSdt("0987654321");

        when(nguoiDungRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(vaiTroRepository.findById("USER")).thenReturn(Optional.of(mockRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(nguoiDungRepository.save(any(NguoiDung.class))).thenReturn(mockUser);

        // Act
        ApiResponse<String> response = authService.registerUser(model);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("thành công");
        verify(nguoiDungRepository, times(1)).save(any(NguoiDung.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("registerUser - duplicate email returns error")
    void registerUser_duplicateEmail_returnsError() {
        // Arrange
        NguoiDungModel model = new NguoiDungModel();
        model.setEmail("existing@example.com");
        model.setMatKhau("password123");

        when(nguoiDungRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act
        ApiResponse<String> response = authService.registerUser(model);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Email");
        verify(nguoiDungRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser - missing USER role returns error")
    void registerUser_missingUserRole_returnsError() {
        // Arrange
        NguoiDungModel model = new NguoiDungModel();
        model.setEmail("newuser@example.com");
        model.setMatKhau("password123");

        when(nguoiDungRepository.existsByEmail(anyString())).thenReturn(false);
        when(vaiTroRepository.findById("USER")).thenReturn(Optional.empty());

        // Act
        ApiResponse<String> response = authService.registerUser(model);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Vai trò");
        verify(nguoiDungRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser - password is BCrypt encoded, not stored in plain text")
    void registerUser_passwordIsEncoded() {
        // Arrange
        NguoiDungModel model = new NguoiDungModel();
        model.setEmail("user@example.com");
        model.setMatKhau("plaintext123");

        when(nguoiDungRepository.existsByEmail(anyString())).thenReturn(false);
        when(vaiTroRepository.findById("USER")).thenReturn(Optional.of(mockRole));
        when(passwordEncoder.encode("plaintext123")).thenReturn("$2a$10$hashed");
        when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(inv -> {
            NguoiDung saved = inv.getArgument(0);
            // Verify the password is NOT saved as plain text
            assertThat(saved.getMatKhau()).isNotEqualTo("plaintext123");
            assertThat(saved.getMatKhau()).startsWith("$2a$10$");
            return saved;
        });

        // Act
        authService.registerUser(model);

        // Assert
        verify(passwordEncoder).encode("plaintext123");
    }

    // ===== validateUser tests =====

    @Test
    @DisplayName("validateUser - correct credentials and active status returns true")
    void validateUser_validActiveUser_returnsTrue() {
        // Arrange
        when(nguoiDungRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", mockUser.getMatKhau())).thenReturn(true);

        // Act
        boolean result = authService.validateUser("test@example.com", "password123");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateUser - wrong password returns false")
    void validateUser_wrongPassword_returnsFalse() {
        // Arrange
        when(nguoiDungRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpassword", mockUser.getMatKhau())).thenReturn(false);

        // Act
        boolean result = authService.validateUser("test@example.com", "wrongpassword");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateUser - locked account returns false even with correct password")
    void validateUser_lockedAccount_returnsFalse() {
        // Arrange
        NguoiDung lockedUser = NguoiDung.builder()
                .email("locked@example.com")
                .matKhau("$2a$10$hashed")
                .trangThai("Khóa")
                .vaiTro(mockRole)
                .build();

        when(nguoiDungRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.matches("password123", lockedUser.getMatKhau())).thenReturn(true);

        // Act
        boolean result = authService.validateUser("locked@example.com", "password123");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateUser - non-existent email returns false")
    void validateUser_emailNotFound_returnsFalse() {
        // Arrange
        when(nguoiDungRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // Act
        boolean result = authService.validateUser("nobody@example.com", "password");

        // Assert
        assertThat(result).isFalse();
    }
}

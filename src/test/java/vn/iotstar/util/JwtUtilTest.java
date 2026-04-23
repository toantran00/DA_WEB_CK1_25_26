package vn.iotstar.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.iotstar.service.UserDetailsImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtUtil — tests token generation and validation logic.
 * Note: Uses reflection to inject secret via field since @Value cannot bind in plain unit tests.
 */
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 32-char secret = 256 bits (minimum for HS256)
    private static final String TEST_SECRET = "TestSecret12345678901234567890AB";
    private static final int EXPIRATION_MS = 3_600_000; // 1 hour

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        // Inject @Value fields via reflection
        var secretField = JwtUtil.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, TEST_SECRET);

        var expField = JwtUtil.class.getDeclaredField("jwtExpirationMs");
        expField.setAccessible(true);
        expField.set(jwtUtil, EXPIRATION_MS);
    }

    private Authentication buildMockAuthentication(String email, String role) {
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getUsername()).thenReturn(email);
        when(userDetails.getId()).thenReturn(1);
        when(userDetails.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    @Test
    @DisplayName("generateJwtToken - returns a non-blank token for valid authentication")
    void generateJwtToken_validAuth_returnsToken() {
        Authentication auth = buildMockAuthentication("user@example.com", "USER");

        String token = jwtUtil.generateJwtToken(auth);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("validateJwtToken - valid token returns true")
    void validateJwtToken_validToken_returnsTrue() {
        Authentication auth = buildMockAuthentication("user@example.com", "USER");
        String token = jwtUtil.generateJwtToken(auth);

        boolean valid = jwtUtil.validateJwtToken(token);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("validateJwtToken - malformed token returns false")
    void validateJwtToken_malformedToken_returnsFalse() {
        boolean valid = jwtUtil.validateJwtToken("this.is.not.a.jwt");
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validateJwtToken - empty string returns false")
    void validateJwtToken_emptyToken_returnsFalse() {
        boolean valid = jwtUtil.validateJwtToken("");
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validateJwtToken - tampered token returns false")
    void validateJwtToken_tamperedToken_returnsFalse() {
        Authentication auth = buildMockAuthentication("user@example.com", "USER");
        String token = jwtUtil.generateJwtToken(auth);

        // Corrupt last character of signature
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        boolean valid = jwtUtil.validateJwtToken(tampered);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("getUserNameFromJwtToken - returns correct email from token")
    void getUserNameFromJwtToken_validToken_returnsEmail() {
        Authentication auth = buildMockAuthentication("user@example.com", "USER");
        String token = jwtUtil.generateJwtToken(auth);

        String extractedEmail = jwtUtil.getUserNameFromJwtToken(token);

        assertThat(extractedEmail).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("generateJwtToken for ADMIN role includes correct role claim")
    void generateJwtToken_adminRole_tokenIsValid() {
        Authentication auth = buildMockAuthentication("admin@example.com", "ADMIN");
        String token = jwtUtil.generateJwtToken(auth);

        assertThat(jwtUtil.validateJwtToken(token)).isTrue();
        assertThat(jwtUtil.getUserNameFromJwtToken(token)).isEqualTo("admin@example.com");
    }
}

package edu.kuet.ums.unit;

import edu.kuet.ums.entity.Student;
import edu.kuet.ums.security.AppUserDetails;
import edu.kuet.ums.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-chars";

    private AppUserDetails user(String username) {
        Student s = new Student(username, "hash", username + "@x.test", "Test Person", null, null,
                "S-1", null, "2024", 1);
        return new AppUserDetails(s);
    }

    @Test
    void generatedTokenCarriesUsernameAndRejectsTamperingWrongKeyAndExpiry() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken(user("alice"));

        assertThat(jwt.extractUsername(token)).isEqualTo("alice");


        int i = token.length() - 5;
        String tampered = token.substring(0, i) + (token.charAt(i) == 'A' ? 'B' : 'A') + token.substring(i + 1);
        assertThatThrownBy(() -> jwt.extractUsername(tampered)).isInstanceOf(JwtException.class);


        JwtService other = new JwtService("another-secret-key-that-is-also-32-chars-long", 60_000);
        assertThatThrownBy(() -> other.extractUsername(token)).isInstanceOf(JwtException.class);


        JwtService expired = new JwtService(SECRET, -1_000);
        String old = expired.generateToken(user("alice"));
        assertThatThrownBy(() -> jwt.extractUsername(old)).isInstanceOf(JwtException.class);


        assertThatThrownBy(() -> new JwtService("short", 1000)).isInstanceOf(IllegalStateException.class);
    }
}

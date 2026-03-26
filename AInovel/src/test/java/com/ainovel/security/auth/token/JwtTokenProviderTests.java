package com.ainovel.security.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ainovel.security.auth.rbac.RoleType;
import com.ainovel.security.config.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTests {

    private static final String JWT_SECRET = "test-secret-key";
    private final JwtTokenProvider tokenProvider =
            new JwtTokenProvider(new SecurityProperties(
                    null,
                    null,
                    null,
                    "ainovel",
                    JWT_SECRET,
                    false,
                    true,
                    20,
                    60,
                    10,
                    60,
                    120,
                    60,
                    40,
                    300,
                    20,
                    300,
                    200,
                    300));

    @Test
    void shouldParseValidToken() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant issuedAt = now.minusSeconds(60);
        Instant expiresAt = now.plusSeconds(600);
        String payload = "{\"sub\":\"1001\",\"roles\":[\"USER\",\"AUTHOR\"],\"jti\":\"jti-1\",\"tv\":3,"
                + "\"iat\":" + issuedAt.getEpochSecond() + ",\"exp\":" + expiresAt.getEpochSecond()
                + ",\"iss\":\"ainovel\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload));

        assertTrue(parsed.isPresent());
        AccessTokenClaims claims = parsed.orElseThrow();
        assertEquals("1001", claims.subject());
        assertEquals(Set.of(RoleType.USER, RoleType.AUTHOR), claims.roles());
        assertEquals("jti-1", claims.jti());
        assertEquals(3L, claims.tokenVersion());
        assertEquals(issuedAt, claims.issuedAt());
        assertEquals(expiresAt, claims.expiresAt());
    }

    @Test
    void shouldReturnEmptyWhenTokenExpired() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"sub\":\"1001\",\"roles\":[\"USER\"],\"jti\":\"jti-2\",\"tv\":1,"
                + "\"iat\":" + now.minusSeconds(120).getEpochSecond() + ",\"exp\":"
                + now.minusSeconds(10).getEpochSecond() + ",\"iss\":\"ainovel\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload));

        assertFalse(parsed.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenTokenFormatIsInvalid() {
        Optional<AccessTokenClaims> parsed = tokenProvider.parse("invalid-token-format");

        assertFalse(parsed.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenIssuerDoesNotMatch() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"sub\":\"1001\",\"roles\":[\"USER\"],\"jti\":\"jti-3\",\"tv\":1,"
                + "\"iat\":" + now.minusSeconds(60).getEpochSecond() + ",\"exp\":"
                + now.plusSeconds(300).getEpochSecond() + ",\"iss\":\"other\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload));

        assertFalse(parsed.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenExpirationIsMissing() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"sub\":\"1001\",\"roles\":[\"USER\"],\"jti\":\"jti-4\",\"tv\":1,"
                + "\"iat\":" + now.minusSeconds(60).getEpochSecond() + ",\"iss\":\"ainovel\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload));

        assertFalse(parsed.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenSubjectIsMissing() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"roles\":[\"USER\"],\"jti\":\"jti-5\",\"tv\":1,"
                + "\"iat\":" + now.minusSeconds(60).getEpochSecond() + ",\"exp\":"
                + now.plusSeconds(300).getEpochSecond() + ",\"iss\":\"ainovel\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload));

        assertFalse(parsed.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenSubjectIsNotNumeric() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"sub\":\"user-1001\",\"roles\":[\"USER\"],\"jti\":\"jti-6\",\"tv\":1,"
                + "\"iat\":" + now.minusSeconds(60).getEpochSecond() + ",\"exp\":"
                + now.plusSeconds(300).getEpochSecond() + ",\"iss\":\"ainovel\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload));

        assertFalse(parsed.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenSignatureIsForged() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = "{\"sub\":\"1001\",\"roles\":[\"USER\"],\"jti\":\"jti-7\",\"tv\":1,"
                + "\"iat\":" + now.minusSeconds(60).getEpochSecond() + ",\"exp\":"
                + now.plusSeconds(300).getEpochSecond() + ",\"iss\":\"ainovel\"}";

        Optional<AccessTokenClaims> parsed = tokenProvider.parse(jwtFromPayload(payload) + "tampered");

        assertFalse(parsed.isPresent());
    }

    private static String jwtFromPayload(String payloadJson) {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String encodedHeader = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return encodedHeader + "." + encodedPayload + "." + sign(encodedHeader + "." + encodedPayload);
    }

    private static String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

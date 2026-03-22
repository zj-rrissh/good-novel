package com.ainovel.security.auth.token;

import com.ainovel.security.auth.rbac.RoleType;
import com.ainovel.security.config.SecurityProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String JWT_ALGORITHM = "HS256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int JWT_PARTS = 3;

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String issueAccessToken(Long userId, Set<RoleType> roles, long tokenVersion, String jti, Instant issuedAt, Instant expiresAt) {
        ObjectNode header = objectMapper.createObjectNode();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sub", String.valueOf(userId));
        payload.put("iss", securityProperties.jwtIssuer());
        payload.put("jti", jti);
        payload.put("tv", tokenVersion);
        payload.put("iat", issuedAt.truncatedTo(ChronoUnit.SECONDS).getEpochSecond());
        payload.put("exp", expiresAt.truncatedTo(ChronoUnit.SECONDS).getEpochSecond());
        ArrayNode roleArray = payload.putArray("roles");
        roles.stream()
                .map(Enum::name)
                .sorted()
                .forEach(roleArray::add);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        return encodedHeader + "." + encodedPayload + "." + sign(encodedHeader + "." + encodedPayload);
    }

    public Optional<AccessTokenClaims> parse(String token) {
        try {
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }
            String[] parts = token.split("\\.");
            if (parts.length != JWT_PARTS) {
                return Optional.empty();
            }

            JsonNode header = readJson(parts[0]);
            if (!JWT_ALGORITHM.equals(header.path("alg").asText())) {
                return Optional.empty();
            }
            if (!hasJwtSecret() || !signatureMatches(parts[0], parts[1], parts[2])) {
                return Optional.empty();
            }

            JsonNode payload = readJson(parts[1]);
            String issuer = payload.path("iss").asText(null);
            if (!Objects.equals(securityProperties.jwtIssuer(), issuer)) {
                return Optional.empty();
            }

            String subject = payload.path("sub").asText(null);
            if (subject == null || subject.isBlank() || !isNumericSubject(subject)) {
                return Optional.empty();
            }

            if (!payload.hasNonNull("exp")) {
                return Optional.empty();
            }
            Instant expiresAt = Instant.ofEpochSecond(payload.get("exp").asLong());
            if (!expiresAt.isAfter(Instant.now())) {
                return Optional.empty();
            }

            Set<RoleType> roles = payload.hasNonNull("roles")
                    ? Arrays.stream(objectMapper.treeToValue(payload.get("roles"), String[].class))
                    .map(RoleType::valueOf)
                    .collect(Collectors.toSet())
                    : Set.of();
            return Optional.of(new AccessTokenClaims(
                    subject,
                    roles,
                    payload.path("jti").asText(null),
                    payload.path("tv").asLong(0),
                    payload.hasNonNull("iat") ? Instant.ofEpochSecond(payload.get("iat").asLong()) : null,
                    expiresAt));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private JsonNode readJson(String tokenPart) throws java.io.IOException {
        byte[] decoded = Base64.getUrlDecoder().decode(tokenPart);
        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }

    private String encodeJson(JsonNode jsonNode) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(jsonNode));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode JWT payload", ex);
        }
    }

    private boolean hasJwtSecret() {
        return securityProperties.jwtSecret() != null && !securityProperties.jwtSecret().isBlank();
    }

    private boolean signatureMatches(String encodedHeader, String encodedPayload, String encodedSignature) {
        String computedSignature = sign(encodedHeader + "." + encodedPayload);
        return MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                encodedSignature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException ex) {
            throw new IllegalStateException("Unable to verify JWT signature", ex);
        }
    }

    private boolean isNumericSubject(String subject) {
        for (int i = 0; i < subject.length(); i++) {
            if (!Character.isDigit(subject.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

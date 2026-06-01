package com.interview.coach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuthProperties properties;

    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("username", username);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(properties.getJwtExpireHours() * 3600L).getEpochSecond());

        String headerPart = base64Url(toJson(header));
        String payloadPart = base64Url(toJson(payload));
        String signaturePart = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    public TokenClaims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                return null;
            }
            Map<?, ?> payload = OBJECT_MAPPER.readValue(base64UrlDecode(parts[1]), Map.class);
            Number exp = (Number) payload.get("exp");
            if (exp == null || Instant.now().getEpochSecond() > exp.longValue()) {
                return null;
            }
            Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
            String username = String.valueOf(payload.get("username"));
            return new TokenClaims(userId, username);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize jwt part", ex);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign token", ex);
        }
    }

    private String base64Url(String value) {
        return base64Url(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    public record TokenClaims(Long userId, String username) {
    }
}

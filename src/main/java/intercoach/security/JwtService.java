package intercoach.security;

import intercoach.model.AppUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Duration tokenTtl;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${intercoach.jwt.secret}") String secret,
            @Value("${intercoach.jwt.expiration-minutes}") long expirationMinutes
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters."
            );
        }

        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtl = Duration.ofMinutes(expirationMinutes);
    }

    public JwtToken generateToken(AppUser user) {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(tokenTtl);

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getUsername());
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole() == null ? "USER" : user.getRole());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(claims);
        String signature = sign(unsignedToken);

        return new JwtToken(unsignedToken + "." + signature, expiresAt);
    }

    public JwtClaims validateToken(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT structure.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException("Invalid JWT signature.");
        }

        JsonNode claims = decodeClaims(parts[1]);
        String username = claims.path("sub").asText(null);
        Instant expiresAt = Instant.ofEpochSecond(claims.path("exp").asLong(0));

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing.");
        }

        if (!expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("JWT is expired.");
        }

        Long userId = claims.hasNonNull("userId")
                ? claims.path("userId").asLong()
                : null;
        String role = claims.path("role").asText("USER");

        return new JwtClaims(
                userId,
                username,
                claims.path("email").asText(null),
                role,
                expiresAt
        );
    }

    private String encodeJson(Map<String, Object> values) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(values);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode JWT.", exception);
        }
    }

    private JsonNode decodeClaims(String encodedClaims) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedClaims);
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to decode JWT claims.");
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature =
                    mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT.", exception);
        }
    }
}

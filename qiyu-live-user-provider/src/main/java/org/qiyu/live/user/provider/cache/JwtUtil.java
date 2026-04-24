package org.qiyu.live.user.provider.cache;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "my-secret-key-123456";
    private static final long EXPIRE_TIME = 30L * 24 * 60 * 60 * 1000;

    public String generateToken(Long userId) {
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRE_TIME);

        return JWT.create()
                .withClaim("userId", userId)
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET));
    }

    public Long getUserIdFromToken(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET))
                .build()
                .verify(token)
                .getClaim("userId")
                .asLong();
    }
}

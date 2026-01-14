package vaultWeb.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

  // Caffeine cache with eviction policy
  private final Cache<String, Bucket> cache =
      Caffeine.newBuilder()
          .expireAfterAccess(Duration.ofMinutes(5)) // cleanup idle entries
          .maximumSize(10_000) // safety limit
          .build();

  @Value("${spring.rateLimitPerMinute}")
  private Integer rateLimit;

  @Autowired private JwtUtil jwtUtil;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String clientId = getClientIpAddress(httpRequest);
    if (clientId == null || clientId.isBlank()) {
      clientId = extractUserIdFromToken(httpRequest);
    }

    if (clientId == null || clientId.isBlank()) {
      clientId = "anonymous";
    }

    Bucket bucket = resolveBucket(clientId);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      httpResponse.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      chain.doFilter(request, response);
    } else {
      httpResponse.setStatus(429);
      httpResponse.addHeader(
          "X-Rate-Limit-Retry-After-Seconds",
          String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
      httpResponse.getWriter().write("Rate limit exceeded");
    }
  }

  private Bucket resolveBucket(String clientId) {
    // Caffeine auto-creates and caches buckets
    return cache.get(clientId, k -> createNewBucket());
  }

  private Bucket createNewBucket() {
    Bandwidth limit =
        Bandwidth.builder()
            .capacity(rateLimit)
            .refillGreedy(rateLimit, Duration.ofMinutes(1))
            .build();

    return Bucket.builder().addLimit(limit).build();
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String extractUserIdFromToken(HttpServletRequest request) {
    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return null;
      }
      String token = authHeader.substring(7);
      return jwtUtil.extractUsername(token);
    } catch (Exception e) {
      return null;
    }
  }
}

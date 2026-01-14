package vaultWeb.security.aspects;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vaultWeb.exceptions.RateLimitExceededException;
import vaultWeb.security.JwtUtil;
import vaultWeb.security.annotations.ApiRateLimit;

@Aspect
@Component
public class RateLimitAspect {

  // Caffeine Cache with eviction
  private final Cache<String, Bucket> cache =
      Caffeine.newBuilder()
          .expireAfterAccess(Duration.ofMinutes(5)) // remove idle buckets
          .maximumSize(10_000) // safety cap
          .build();

  @Autowired private JwtUtil jwtUtil;

  @Around("@annotation(apiRateLimit)")
  public Object rateLimit(ProceedingJoinPoint joinPoint, ApiRateLimit apiRateLimit)
      throws Throwable {

    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    String key = getRateLimitKey(joinPoint, request, apiRateLimit);
    Bucket bucket = resolveBucket(key, apiRateLimit);

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      request.setAttribute("X-Rate-Limit-Remaining", probe.getRemainingTokens());
      return joinPoint.proceed();
    } else {
      long retryAfter = probe.getNanosToWaitForRefill() / 1_000_000_000;
      throw new RateLimitExceededException(
          "Rate limit exceeded. Try again in " + retryAfter + " seconds", retryAfter);
    }
  }

  private Bucket resolveBucket(String key, ApiRateLimit rateLimit) {
    return cache.get(
        key,
        k -> {
          Bandwidth limit =
              Bandwidth.builder()
                  .capacity(rateLimit.capacity())
                  .refillGreedy(
                      rateLimit.refillTokens(),
                      Duration.ofMinutes(rateLimit.refillDurationMinutes()))
                  .build();

          return Bucket.builder().addLimit(limit).build();
        });
  }

  private String getRateLimitKey(
      ProceedingJoinPoint joinPoint, HttpServletRequest request, ApiRateLimit rateLimit) {

    String methodName = joinPoint.getSignature().toShortString();

    if (rateLimit.useIpAddress()) {
      String ipAddress = getClientIpAddress(request);
      return methodName + ":" + ipAddress;
    } else {
      String userId = extractUserIdFromToken(request);
      return methodName + ":" + (userId != null ? userId : "anonymous");
    }
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

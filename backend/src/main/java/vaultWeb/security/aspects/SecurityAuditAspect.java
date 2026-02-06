package vaultWeb.security.aspects;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vaultWeb.dtos.user.UserDto;
import vaultWeb.security.JwtUtil;
import vaultWeb.security.annotations.AuditSecurityEvent;
import vaultWeb.security.annotations.SecurityEventType;

/**
 * Aspect that logs security-relevant events for audit purposes.
 *
 * <p>This aspect intercepts methods annotated with {@link AuditSecurityEvent} and logs details
 * including username, event type, timestamp, IP address, and success/failure status.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecurityAuditAspect {

  private static final Logger log = LoggerFactory.getLogger(SecurityAuditAspect.class);
  private final JwtUtil jwtUtil;

  /**
   * Logs successful security operations.
   *
   * @param joinPoint the join point providing access to the method being invoked
   * @param auditSecurityEvent the annotation containing the event type
   */
  @AfterReturning("@annotation(auditSecurityEvent)")
  public void logSuccess(JoinPoint joinPoint, AuditSecurityEvent auditSecurityEvent) {
    logSecurityEvent(joinPoint, auditSecurityEvent.value(), "SUCCESS", null);
  }

  /**
   * Logs failed security operations.
   *
   * @param joinPoint the join point providing access to the method being invoked
   * @param auditSecurityEvent the annotation containing the event type
   * @param ex the exception that was thrown
   */
  @AfterThrowing(pointcut = "@annotation(auditSecurityEvent)", throwing = "ex")
  public void logFailure(JoinPoint joinPoint, AuditSecurityEvent auditSecurityEvent, Exception ex) {
    logSecurityEvent(joinPoint, auditSecurityEvent.value(), "FAILURE", ex);
  }

  private void logSecurityEvent(
      JoinPoint joinPoint, SecurityEventType eventType, String status, Exception ex) {
    HttpServletRequest request = getRequest();
    String ip = getClientIp(request);
    String username = extractUsername(joinPoint, request);
    Instant timestamp = Instant.now();

    if (ex == null) {
      log.info(
          "SECURITY_EVENT: type={}, username={}, ip={}, timestamp={}, status={}",
          eventType,
          username,
          ip,
          timestamp,
          status);
    } else {
      log.warn(
          "SECURITY_EVENT: type={}, username={}, ip={}, timestamp={}, status={}, error={}",
          eventType,
          username,
          ip,
          timestamp,
          status,
          ex.getClass().getSimpleName());
    }
  }

  private String extractUsername(JoinPoint joinPoint, HttpServletRequest request) {
    // First try JWT (for authenticated endpoints: logout, refresh, changePassword)
    if (request != null) {
      String username = jwtUtil.extractUsernameFromRequest(request);
      if (username != null && !username.isBlank()) {
        return username;
      }
    }

    // Fallback: extract from method arguments (for login/register)
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof UserDto userDto && userDto.getUsername() != null) {
        return userDto.getUsername();
      }
    }

    return "anonymous";
  }

  /**
   * Extracts client IP address from the request.
   *
   * <p>Note: X-Forwarded-For header is trusted for proxy environments. This assumes a properly
   * configured reverse proxy (nginx, AWS ALB) that overwrites this header from untrusted sources.
   * For maximum security, ensure your proxy strips X-Forwarded-For from incoming requests.
   */
  private String getClientIp(HttpServletRequest request) {
    if (request == null) {
      return "unknown";
    }
    // Support for reverse proxy (nginx, AWS ALB)
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private HttpServletRequest getRequest() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      return attrs.getRequest();
    } catch (IllegalStateException e) {
      log.warn("Unable to retrieve HttpServletRequest context for security audit");
      return null;
    }
  }
}

package vaultWeb.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vaultWeb.dtos.dashboard.UserDashboardDto;
import vaultWeb.exceptions.UnauthorizedException;
import vaultWeb.exceptions.notfound.UserNotFoundException;
import vaultWeb.models.User;
import vaultWeb.repositories.UserRepository;
import vaultWeb.services.DashboardService;
import vaultWeb.services.auth.AuthService;

/** Provides aggregated user-centric data to power the dashboard UI. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Controller", description = "Aggregated view of user related data")
public class DashboardController {

  private final DashboardService dashboardService;
  private final AuthService authService;
  private final UserRepository userRepository;

  @GetMapping("/me")
  @Operation(summary = "Get dashboard data for the authenticated user")
  public ResponseEntity<UserDashboardDto> getCurrentUserDashboard() {
    User currentUser = authService.getCurrentUser();
    if (currentUser == null) {
      throw new UnauthorizedException("User is not authenticated");
    }
    return ResponseEntity.ok(dashboardService.buildDashboard(currentUser));
  }

  @GetMapping("/{username}")
  @Operation(
      summary = "Get dashboard data for a specific user",
      description = "Primarily useful for admin tooling or debugging in the UI.")
  public ResponseEntity<UserDashboardDto> getDashboardForUser(@PathVariable String username) {
    User targetUser =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    return ResponseEntity.ok(dashboardService.buildDashboard(targetUser));
  }
}

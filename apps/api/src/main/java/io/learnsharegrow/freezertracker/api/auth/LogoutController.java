package io.learnsharegrow.freezertracker.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogoutController {
  private final AuthService authService;

  public LogoutController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    authService.logout();
    return ResponseEntity.noContent().build();
  }
}

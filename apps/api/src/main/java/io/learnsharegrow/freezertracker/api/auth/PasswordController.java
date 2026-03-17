package io.learnsharegrow.freezertracker.api.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordController {
  private final AuthService authService;

  public PasswordController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/account/password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/account/house")
  public AccountHouseResponse getAccountHouse() {
    return authService.getHouse();
  }
}

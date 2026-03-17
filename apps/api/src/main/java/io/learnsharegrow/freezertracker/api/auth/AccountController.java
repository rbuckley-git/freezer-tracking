package io.learnsharegrow.freezertracker.api.auth;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {
  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public List<AccountResponse> listAccounts() {
    return accountService.listAccounts();
  }

  @GetMapping("/{id}")
  public AccountResponse getAccount(@PathVariable UUID id) {
    return accountService.getAccount(id);
  }

  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreateRequest request) {
    AccountResponse response = accountService.createAccount(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  public AccountResponse updateAccount(@PathVariable UUID id, @Valid @RequestBody AccountUpdateRequest request) {
    return accountService.updateAccount(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
    accountService.deleteAccount(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/api-keys")
  public List<AccountApiKeyResponse> listAccountApiKeys(@PathVariable UUID id) {
    return accountService.listAccountApiKeys(id);
  }

  @PostMapping("/{id}/api-keys/revoke")
  public ResponseEntity<Void> revokeAccountApiKey(
      @PathVariable UUID id,
      @Valid @RequestBody RevokeApiKeyRequest request) {
    accountService.revokeApiKey(id, request.getApiKey());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/api-keys/revoke-all")
  public ResponseEntity<Void> revokeAllAccountApiKeys(@PathVariable UUID id) {
    accountService.revokeAllApiKeys(id);
    return ResponseEntity.noContent().build();
  }
}

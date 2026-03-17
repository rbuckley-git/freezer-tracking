package io.learnsharegrow.freezertracker.api.security;

import io.learnsharegrow.freezertracker.api.auth.Account;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKey;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKeyRepository;
import io.learnsharegrow.freezertracker.api.auth.ApiKeyHasher;
import io.learnsharegrow.freezertracker.api.auth.AccountRepository;
import io.learnsharegrow.freezertracker.api.common.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {
  private static final String HEADER = "X-API-Key";
  private static final Set<String> OPEN_PATHS = Set.of("/login", "/health", "/version");

  private final AccountRepository accountRepository;
  private final AccountApiKeyRepository accountApiKeyRepository;

  public ApiKeyInterceptor(AccountRepository accountRepository, AccountApiKeyRepository accountApiKeyRepository) {
    this.accountRepository = accountRepository;
    this.accountApiKeyRepository = accountApiKeyRepository;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String path = request.getRequestURI();
    if (OPEN_PATHS.contains(path)) {
      return true;
    }
    String value = request.getHeader(HEADER);
    if (value == null || value.isBlank()) {
      throw new ForbiddenException();
    }
    String token = value.trim();
    UUID legacyApiKey = parseUuidOrNull(token);
    Instant now = Instant.now();
    String apiKeyHash = ApiKeyHasher.sha256(token);
    AccountApiKey accountApiKey = accountApiKeyRepository
        .findByApiKeyHashAndExpiresAtAfterAndRevokedAtIsNull(apiKeyHash, now)
        .or(() -> legacyApiKey == null
            ? java.util.Optional.empty()
            : accountApiKeyRepository.findByApiKeyAndExpiresAtAfterAndRevokedAtIsNull(legacyApiKey, now))
        .orElseThrow(ForbiddenException::new);
    Account account = accountRepository.findById(accountApiKey.getAccountId())
        .orElseThrow(ForbiddenException::new);
    accountApiKeyRepository.touchLastUsedAt(accountApiKey.getApiKey(), now);
    AccountContext.setAccountId(account.getId());
    AccountContext.setHouseId(account.getHouseId());
    AccountContext.setApiKey(accountApiKey.getApiKey());
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    AccountContext.clear();
  }

  private UUID parseUuidOrNull(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}

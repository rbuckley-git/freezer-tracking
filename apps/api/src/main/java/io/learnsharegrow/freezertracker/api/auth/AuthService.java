package io.learnsharegrow.freezertracker.api.auth;

import io.learnsharegrow.freezertracker.api.common.ForbiddenException;
import io.learnsharegrow.freezertracker.api.common.BadRequestException;
import io.learnsharegrow.freezertracker.api.common.NotFoundException;
import io.learnsharegrow.freezertracker.api.config.ConfigService;
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import io.learnsharegrow.freezertracker.api.security.AccountContext;
import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
  private final AccountRepository accountRepository;
  private final AccountApiKeyRepository accountApiKeyRepository;
  private final ConfigService configService;
  private final HouseRepository houseRepository;
  private final String pepper;

  public AuthService(AccountRepository accountRepository, AccountApiKeyRepository accountApiKeyRepository,
      ConfigService configService,
      HouseRepository houseRepository,
      @Value("${security.password-pepper:}") String pepper) {
    this.accountRepository = accountRepository;
    this.accountApiKeyRepository = accountApiKeyRepository;
    this.configService = configService;
    this.houseRepository = houseRepository;
    this.pepper = pepper;
  }

  @Transactional(noRollbackFor = ForbiddenException.class)
  public LoginResponse login(LoginRequest request) {
    if (pepper == null || pepper.isBlank()) {
      logger.error("Login refused because PASSWORD_PEPPER is not configured");
      throw new IllegalStateException("PASSWORD_PEPPER is not configured");
    }
    Account account = accountRepository.findByUsernameIgnoreCase(request.getUsername())
        .orElseThrow(() -> {
          logger.warn("Login refused: account not found for username {}", request.getUsername());
          return new ForbiddenException();
        });
    Instant now = Instant.now();
    if (isLockedOut(account, now)) {
      long minutesRemaining = Math.max(1, Duration.between(now, account.getLockoutUntil()).toMinutes());
      logger.warn(
          "Login refused due to lockout for account {} (minutesRemaining={})",
          account.getId(),
          minutesRemaining);
      throw new ForbiddenException();
    }
    boolean matches =
        PasswordHasher.matches(request.getPassword(), pepper, account.getPasswordHash(), account.getPasswordSalt());
    if (!matches) {
      logger.warn("Login refused: password mismatch for account {}", account.getId());
      registerFailedLogin(account, now);
      throw new ForbiddenException();
    }
    logger.info(
        "Login success for account {} (clientType={}, admin={}, superAdmin={})",
        account.getId(),
        request.getClientType(),
        account.isAdmin(),
        account.isSuperAdmin());
    accountApiKeyRepository.deleteExpiredOrRevoked(now);
    accountApiKeyRepository.revokeActiveByAccountIdAndClientType(account.getId(), request.getClientType(), now);
    UUID apiKey = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    int expiryDays = configService.getIntValue("api_key_expiry_days", 30);
    Instant expiry = Instant.now().plus(expiryDays, ChronoUnit.DAYS);
    AccountApiKey key = new AccountApiKey();
    key.setApiKey(apiKeyId);
    key.setApiKeyHash(ApiKeyHasher.sha256(apiKey.toString()));
    key.setAccountId(account.getId());
    key.setCreatedAt(now);
    key.setExpiresAt(expiry);
    key.setClientType(request.getClientType());
    accountApiKeyRepository.save(key);
    account.setFailedLoginCount(0);
    account.setLockoutUntil(null);
    accountRepository.save(account);
    return new LoginResponse(apiKey, expiry, account.isAdmin(), account.isSuperAdmin());
  }

  @Transactional
  public void logout() {
    UUID accountId = AccountContext.requireAccountId();
    UUID apiKey = AccountContext.requireApiKey();
    accountRepository.findById(accountId).orElseThrow(ForbiddenException::new);
    accountApiKeyRepository.revoke(apiKey, Instant.now());
    logger.info("Logout success for account {}", accountId);
  }

  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    if (pepper == null || pepper.isBlank()) {
      logger.error("Password change refused because PASSWORD_PEPPER is not configured");
      throw new IllegalStateException("PASSWORD_PEPPER is not configured");
    }
    UUID accountId = AccountContext.requireAccountId();
    Account account = accountRepository.findById(accountId).orElseThrow(ForbiddenException::new);
    boolean matches =
        PasswordHasher.matches(request.getCurrentPassword(), pepper, account.getPasswordHash(), account.getPasswordSalt());
    if (!matches) {
      logger.warn("Password change refused: current password mismatch for account {}", account.getId());
      throw new BadRequestException("Current password is incorrect");
    }
    String newHash = PasswordHasher.bcrypt(request.getNewPassword() + pepper);
    account.setPasswordSalt("");
    account.setPasswordHash(newHash);
    accountRepository.save(account);
    logger.info("Password changed for account {}", account.getId());
  }

  private boolean isLockedOut(Account account, Instant now) {
    Instant lockoutUntil = account.getLockoutUntil();
    return lockoutUntil != null && lockoutUntil.isAfter(now);
  }

  private void registerFailedLogin(Account account, Instant now) {
    int current = account.getFailedLoginCount() == null ? 0 : account.getFailedLoginCount();
    int updated = current + 1;
    account.setFailedLoginCount(updated);
    if (updated % 3 == 0) {
      int batch = updated / 3;
      long minutes = 10L * (1L << (batch - 1));
      account.setLockoutUntil(now.plus(minutes, ChronoUnit.MINUTES));
    }
    accountRepository.save(account);
  }

  @Transactional(readOnly = true)
  public AccountHouseResponse getHouse() {
    UUID accountId = AccountContext.requireAccountId();
    Account account = accountRepository.findById(accountId).orElseThrow(ForbiddenException::new);
    UUID houseId = account.getHouseId();
    if (houseId == null) {
      throw new BadRequestException("House is not set for this account");
    }
    House house = houseRepository.findById(houseId).orElseThrow(() -> NotFoundException.house(houseId));
    return new AccountHouseResponse(house.getId(), house.getName());
  }
}

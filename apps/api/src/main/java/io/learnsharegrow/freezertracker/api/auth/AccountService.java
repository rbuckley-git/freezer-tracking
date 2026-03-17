package io.learnsharegrow.freezertracker.api.auth;

import io.learnsharegrow.freezertracker.api.common.ForbiddenException;
import io.learnsharegrow.freezertracker.api.common.NotFoundException;
import io.learnsharegrow.freezertracker.api.common.BadRequestException;
import io.learnsharegrow.freezertracker.api.common.DomainLimitService;
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository accountRepository;
  private final AccountApiKeyRepository accountApiKeyRepository;
  private final AdminGuard adminGuard;
  private final HouseRepository houseRepository;
  private final DomainLimitService domainLimitService;
  private final String pepper;

  public AccountService(
      AccountRepository accountRepository,
      AccountApiKeyRepository accountApiKeyRepository,
      AdminGuard adminGuard,
      HouseRepository houseRepository,
      DomainLimitService domainLimitService,
      @Value("${security.password-pepper:}") String pepper) {
    this.accountRepository = accountRepository;
    this.accountApiKeyRepository = accountApiKeyRepository;
    this.adminGuard = adminGuard;
    this.houseRepository = houseRepository;
    this.domainLimitService = domainLimitService;
    this.pepper = pepper;
  }

  @Transactional(readOnly = true)
  public List<AccountResponse> listAccounts() {
    Account actor = adminGuard.requireAdmin();
    if (actor.isSuperAdmin()) {
      return accountRepository.findAll().stream().map(this::toResponse).toList();
    }
    return accountRepository.findAllByHouseId(actor.getHouseId()).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public AccountResponse getAccount(UUID id) {
    Account actor = adminGuard.requireAdmin();
    Account account = findManageableAccount(actor, id);
    return toResponse(account);
  }

  @Transactional
  public AccountResponse createAccount(AccountCreateRequest request) {
    Account actor = adminGuard.requireAdmin();
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalStateException("PASSWORD_PEPPER is not configured");
    }
    UUID houseId = resolveTargetHouseId(actor, request.getHouseId());
    boolean isAdmin = resolveRequestedAdmin(actor, request.isAdmin(), request.isSuperAdmin());
    boolean isSuperAdmin = resolveRequestedSuperAdmin(actor, request.isAdmin(), request.isSuperAdmin());
    House house = houseRepository.findById(houseId)
        .orElseThrow(() -> NotFoundException.house(houseId));
    ensureHouseHasAccountCapacity(house.getId());
    String hash = PasswordHasher.bcrypt(request.getPassword() + pepper);

    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(request.getUsername());
    account.setPasswordSalt("");
    account.setPasswordHash(hash);
    account.setAdmin(isAdmin);
    account.setSuperAdmin(isSuperAdmin);
    account.setHouseId(house.getId());
    account.setFailedLoginCount(0);
    account.setLockoutUntil(null);

    return toResponse(accountRepository.save(account));
  }

  @Transactional
  public AccountResponse updateAccount(UUID id, AccountUpdateRequest request) {
    Account actor = adminGuard.requireAdmin();
    Account account = findManageableAccount(actor, id);
    UUID houseId = resolveTargetHouseId(actor, request.getHouseId());
    boolean isAdmin = resolveRequestedAdmin(actor, request.isAdmin(), request.isSuperAdmin());
    boolean isSuperAdmin = resolveRequestedSuperAdmin(actor, request.isAdmin(), request.isSuperAdmin());
    House house = houseRepository.findById(houseId)
        .orElseThrow(() -> NotFoundException.house(houseId));
    if (!house.getId().equals(account.getHouseId())) {
      ensureHouseHasAccountCapacity(house.getId());
    }
    account.setUsername(request.getUsername());
    account.setAdmin(isAdmin);
    account.setSuperAdmin(isSuperAdmin);
    account.setHouseId(house.getId());
    return toResponse(accountRepository.save(account));
  }

  @Transactional
  public void deleteAccount(UUID id) {
    Account admin = adminGuard.requireAdmin();
    if (admin.getId().equals(id)) {
      throw new ForbiddenException();
    }
    Account account = findManageableAccount(admin, id);
    accountApiKeyRepository.deleteByAccountId(id);
    accountRepository.delete(account);
  }

  @Transactional
  public void revokeAllApiKeys(UUID id) {
    Account actor = adminGuard.requireAdmin();
    findManageableAccount(actor, id);
    accountApiKeyRepository.revokeAllByAccountId(id, java.time.Instant.now());
  }

  @Transactional(readOnly = true)
  public List<AccountApiKeyResponse> listAccountApiKeys(UUID id) {
    Account actor = adminGuard.requireAdmin();
    findManageableAccount(actor, id);
    return accountApiKeyRepository.findByAccountIdOrderByCreatedAtDesc(id).stream()
        .map(this::toApiKeyResponse)
        .toList();
  }

  @Transactional
  public void revokeApiKey(UUID id, String apiKeyValue) {
    Account actor = adminGuard.requireAdmin();
    findManageableAccount(actor, id);
    UUID apiKey = parseApiKey(apiKeyValue);
    accountApiKeyRepository.revokeByAccountIdAndApiKey(id, apiKey, java.time.Instant.now());
  }

  private AccountResponse toResponse(Account account) {
    AccountResponse response = new AccountResponse();
    response.setId(account.getId());
    response.setUsername(account.getUsername());
    response.setAdmin(account.isAdmin());
    response.setSuperAdmin(account.isSuperAdmin());
    response.setHouseId(account.getHouseId());
    return response;
  }

  private AccountApiKeyResponse toApiKeyResponse(AccountApiKey apiKey) {
    AccountApiKeyResponse response = new AccountApiKeyResponse();
    response.setApiKey(apiKey.getApiKey());
    response.setCreatedAt(apiKey.getCreatedAt());
    response.setExpiresAt(apiKey.getExpiresAt());
    response.setLastUsedAt(apiKey.getLastUsedAt());
    response.setRevokedAt(apiKey.getRevokedAt());
    response.setDeviceLabel(apiKey.getDeviceLabel());
    response.setClientType(apiKey.getClientType());
    return response;
  }

  private UUID parseHouseId(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("House ID is invalid");
    }
  }

  private UUID parseApiKey(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("API key is invalid");
    }
  }

  private void ensureHouseHasAccountCapacity(UUID houseId) {
    int maxAccountsPerHouse = domainLimitService.maxAccountsPerHouse();
    if (accountRepository.countByHouseId(houseId) >= maxAccountsPerHouse) {
      throw new BadRequestException("House cannot have more than " + maxAccountsPerHouse + " accounts");
    }
  }

  private UUID resolveTargetHouseId(Account actor, String requestedHouseIdValue) {
    UUID requestedHouseId = parseHouseId(requestedHouseIdValue);
    if (actor.isSuperAdmin()) {
      return requestedHouseId;
    }
    if (!requestedHouseId.equals(actor.getHouseId())) {
      throw new ForbiddenException();
    }
    return requestedHouseId;
  }

  private boolean resolveRequestedAdmin(Account actor, boolean requestedAdmin, boolean requestedSuperAdmin) {
    if (actor.isSuperAdmin()) {
      return requestedAdmin;
    }
    if (requestedAdmin || requestedSuperAdmin) {
      throw new ForbiddenException();
    }
    return false;
  }

  private boolean resolveRequestedSuperAdmin(Account actor, boolean requestedAdmin, boolean requestedSuperAdmin) {
    if (!requestedSuperAdmin) {
      return false;
    }
    if (!requestedAdmin) {
      throw new BadRequestException("Super-admin accounts must also be admin accounts");
    }
    if (!actor.isSuperAdmin()) {
      throw new ForbiddenException();
    }
    return true;
  }

  private Account findManageableAccount(Account actor, UUID targetAccountId) {
    Account target = accountRepository.findById(targetAccountId).orElseThrow(() -> NotFoundException.account(targetAccountId));
    if (actor.isSuperAdmin()) {
      return target;
    }
    if (!actor.getHouseId().equals(target.getHouseId())) {
      throw new ForbiddenException();
    }
    if (target.isAdmin() || target.isSuperAdmin()) {
      throw new ForbiddenException();
    }
    return target;
  }
}

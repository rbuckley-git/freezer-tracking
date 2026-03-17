package io.learnsharegrow.freezertracker.api.auth;

import io.learnsharegrow.freezertracker.api.common.BadRequestException;
import io.learnsharegrow.freezertracker.api.common.DomainLimitService;
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapAdminService {
  private final AccountRepository accountRepository;
  private final HouseRepository houseRepository;
  private final DomainLimitService domainLimitService;
  private final String pepper;

  public BootstrapAdminService(
      AccountRepository accountRepository,
      HouseRepository houseRepository,
      DomainLimitService domainLimitService,
      @Value("${security.password-pepper:}") String pepper) {
    this.accountRepository = accountRepository;
    this.houseRepository = houseRepository;
    this.domainLimitService = domainLimitService;
    this.pepper = pepper;
  }

  @Transactional
  public BootstrapAdminResult bootstrapAdmin(String usernameRaw, String passwordRaw, String houseNameRaw) {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalStateException("PASSWORD_PEPPER is not configured");
    }

    String username = normalise(usernameRaw);
    String password = normalise(passwordRaw);
    String houseName = normalise(houseNameRaw);

    validateUsername(username);
    validatePassword(password);
    validateHouseName(houseName);

    HouseResolution houseResolution = resolveHouse(houseName);

    return accountRepository.findByUsernameIgnoreCase(username)
        .map(account -> {
          boolean changed = false;
          if (!account.isAdmin()) {
            account.setAdmin(true);
            changed = true;
          }
          if (!account.isSuperAdmin()) {
            account.setSuperAdmin(true);
            changed = true;
          }
          if (!houseResolution.house().getId().equals(account.getHouseId())) {
            ensureHouseHasAccountCapacity(houseResolution.house().getId());
            account.setHouseId(houseResolution.house().getId());
            changed = true;
          }
          if (changed) {
            accountRepository.save(account);
          }
          return new BootstrapAdminResult(
              account.getId(),
              false,
              houseResolution.house().getId(),
              houseResolution.houseCreated());
        })
        .orElseGet(() -> createAdmin(username, password, houseResolution));
  }

  private BootstrapAdminResult createAdmin(String username, String password, HouseResolution houseResolution) {
    ensureHouseHasAccountCapacity(houseResolution.house().getId());
    String hash = PasswordHasher.bcrypt(password + pepper);
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setPasswordSalt("");
    account.setPasswordHash(hash);
    account.setAdmin(true);
    account.setSuperAdmin(true);
    account.setHouseId(houseResolution.house().getId());
    account.setFailedLoginCount(0);
    account.setLockoutUntil(null);
    Account saved = accountRepository.save(account);
    return new BootstrapAdminResult(
        saved.getId(),
        true,
        houseResolution.house().getId(),
        houseResolution.houseCreated());
  }

  private HouseResolution resolveHouse(String houseName) {
    return houseRepository.findByNameIgnoreCase(houseName)
        .map(house -> new HouseResolution(house, false))
        .orElseGet(() -> {
          House house = new House();
          house.setId(UUID.randomUUID());
          house.setName(houseName);
          House saved = houseRepository.save(house);
          return new HouseResolution(saved, true);
        });
  }

  private void validateUsername(String username) {
    if (username.isBlank()) {
      throw new BadRequestException("Bootstrap username is required");
    }
    if (username.length() > 254) {
      throw new BadRequestException("Bootstrap username must be 254 characters or fewer");
    }
    if (containsUnsafeChars(username)) {
      throw new BadRequestException("Bootstrap username contains invalid characters");
    }
    if (!username.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      throw new BadRequestException("Bootstrap username must be a valid email address");
    }
  }

  private void validatePassword(String password) {
    if (password.isBlank()) {
      throw new BadRequestException("Bootstrap password is required");
    }
    if (password.length() < 12 || password.length() > 128) {
      throw new BadRequestException("Bootstrap password must be between 12 and 128 characters");
    }
  }

  private void validateHouseName(String houseName) {
    if (houseName.isBlank()) {
      throw new BadRequestException("Bootstrap house name is required");
    }
    if (houseName.length() > 255) {
      throw new BadRequestException("Bootstrap house name must be 255 characters or fewer");
    }
    if (containsUnsafeChars(houseName)) {
      throw new BadRequestException("Bootstrap house name contains invalid characters");
    }
  }

  private String normalise(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean containsUnsafeChars(String value) {
    return value.contains("<") || value.contains(">") || value.contains("\"") || value.contains("'");
  }

  private void ensureHouseHasAccountCapacity(UUID houseId) {
    int maxAccountsPerHouse = domainLimitService.maxAccountsPerHouse();
    if (accountRepository.countByHouseId(houseId) >= maxAccountsPerHouse) {
      throw new BadRequestException("House cannot have more than " + maxAccountsPerHouse + " accounts");
    }
  }

  private record HouseResolution(House house, boolean houseCreated) {}
}

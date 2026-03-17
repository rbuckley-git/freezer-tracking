package io.learnsharegrow.freezertracker.api.auth;

import io.learnsharegrow.freezertracker.api.common.ForbiddenException;
import io.learnsharegrow.freezertracker.api.security.AccountContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {
  private final AccountRepository accountRepository;

  public AdminGuard(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public Account requireAdmin() {
    UUID accountId = AccountContext.requireAccountId();
    Account account = accountRepository.findById(accountId).orElseThrow(ForbiddenException::new);
    if (!account.isAdmin()) {
      throw new ForbiddenException();
    }
    return account;
  }

  public Account requireSuperAdmin() {
    Account account = requireAdmin();
    if (!account.isSuperAdmin()) {
      throw new ForbiddenException();
    }
    return account;
  }
}

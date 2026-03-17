package io.learnsharegrow.freezertracker.api.security;

import io.learnsharegrow.freezertracker.api.common.ForbiddenException;
import java.util.Optional;
import java.util.UUID;

public final class AccountContext {
  private static final ThreadLocal<UUID> ACCOUNT_ID = new ThreadLocal<>();
  private static final ThreadLocal<UUID> HOUSE_ID = new ThreadLocal<>();
  private static final ThreadLocal<UUID> API_KEY = new ThreadLocal<>();

  private AccountContext() {}

  public static void setAccountId(UUID accountId) {
    ACCOUNT_ID.set(accountId);
  }

  public static void setHouseId(UUID houseId) {
    HOUSE_ID.set(houseId);
  }

  public static void setApiKey(UUID apiKey) {
    API_KEY.set(apiKey);
  }

  public static Optional<UUID> getAccountId() {
    return Optional.ofNullable(ACCOUNT_ID.get());
  }

  public static Optional<UUID> getHouseId() {
    return Optional.ofNullable(HOUSE_ID.get());
  }

  public static Optional<UUID> getApiKey() {
    return Optional.ofNullable(API_KEY.get());
  }

  public static UUID requireHouseId() {
    return getHouseId().orElseThrow(ForbiddenException::new);
  }

  public static UUID requireAccountId() {
    return getAccountId().orElseThrow(ForbiddenException::new);
  }

  public static UUID requireApiKey() {
    return getApiKey().orElseThrow(ForbiddenException::new);
  }

  public static void clear() {
    ACCOUNT_ID.remove();
    HOUSE_ID.remove();
    API_KEY.remove();
  }
}

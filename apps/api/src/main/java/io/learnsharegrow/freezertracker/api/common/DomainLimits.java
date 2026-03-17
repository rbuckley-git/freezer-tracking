package io.learnsharegrow.freezertracker.api.common;

public final class DomainLimits {
  public static final String MAX_FREEZERS_PER_HOUSE_KEY = "limits.house.max-freezers";
  public static final String MAX_ACCOUNTS_PER_HOUSE_KEY = "limits.house.max-accounts";
  public static final String MAX_ITEMS_PER_SHELF_KEY = "limits.shelf.max-items";

  public static final int MAX_FREEZERS_PER_HOUSE = 10;
  public static final int MAX_ACCOUNTS_PER_HOUSE = 10;
  public static final int MAX_ITEMS_PER_SHELF = 100;

  private DomainLimits() {}
}

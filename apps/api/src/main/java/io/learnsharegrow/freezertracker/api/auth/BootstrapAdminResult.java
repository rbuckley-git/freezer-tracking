package io.learnsharegrow.freezertracker.api.auth;

import java.util.UUID;

public record BootstrapAdminResult(UUID accountId, boolean accountCreated, UUID houseId, boolean houseCreated) {}

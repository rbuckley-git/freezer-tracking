package io.learnsharegrow.freezertracker.api.auth;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  Optional<Account> findByUsernameIgnoreCase(String username);

  long countByHouseId(UUID houseId);

  List<Account> findAllByHouseId(UUID houseId);

  Optional<Account> findByIdAndHouseId(UUID id, UUID houseId);
}

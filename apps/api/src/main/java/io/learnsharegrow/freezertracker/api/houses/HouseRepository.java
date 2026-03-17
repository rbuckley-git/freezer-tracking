package io.learnsharegrow.freezertracker.api.houses;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, UUID> {
  Optional<House> findByNameIgnoreCase(String name);
}

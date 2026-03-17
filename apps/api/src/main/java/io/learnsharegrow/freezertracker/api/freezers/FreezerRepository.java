package io.learnsharegrow.freezertracker.api.freezers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreezerRepository extends JpaRepository<Freezer, Integer> {
  List<Freezer> findAllByHouseId(UUID houseId);

  Optional<Freezer> findByIdAndHouseId(Integer id, UUID houseId);

  boolean existsByHouseIdAndName(UUID houseId, String name);

  boolean existsByHouseIdAndNameAndIdNot(UUID houseId, String name, Integer id);

  long countByHouseId(UUID houseId);
}

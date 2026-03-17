package io.learnsharegrow.freezertracker.api.items;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, UUID> {
  interface ShelfItemCount {
    Integer getFreezerId();
    Integer getShelfNumber();
    Long getItemCount();
  }

  Page<Item> findAllByHouseId(UUID houseId, Pageable pageable);

  Optional<Item> findByIdAndHouseId(UUID id, UUID houseId);

  long countByHouseIdAndFreezerIdAndShelfNumber(UUID houseId, Integer freezerId, Integer shelfNumber);

  boolean existsByHouseIdAndFreezerId(UUID houseId, Integer freezerId);

  @Query("""
      select i.freezerId as freezerId, i.shelfNumber as shelfNumber, count(i) as itemCount
      from Item i
      group by i.freezerId, i.shelfNumber
      """)
  List<ShelfItemCount> countItemsByFreezerAndShelf();

  @Query("select max(i.reference) from Item i where i.houseId = :houseId")
  Optional<String> findMaxReferenceByHouseId(@Param("houseId") UUID houseId);
}

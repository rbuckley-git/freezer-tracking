package io.learnsharegrow.freezertracker.api.statistics;

import io.learnsharegrow.freezertracker.api.auth.AdminGuard;
import io.learnsharegrow.freezertracker.api.freezers.Freezer;
import io.learnsharegrow.freezertracker.api.freezers.FreezerRepository;
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import io.learnsharegrow.freezertracker.api.items.ItemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
  private final HouseRepository houseRepository;
  private final FreezerRepository freezerRepository;
  private final ItemRepository itemRepository;
  private final AdminGuard adminGuard;

  public StatisticsService(
      HouseRepository houseRepository,
      FreezerRepository freezerRepository,
      ItemRepository itemRepository,
      AdminGuard adminGuard) {
    this.houseRepository = houseRepository;
    this.freezerRepository = freezerRepository;
    this.itemRepository = itemRepository;
    this.adminGuard = adminGuard;
  }

  @Transactional(readOnly = true)
  public StatisticsResponse getStatistics() {
    adminGuard.requireSuperAdmin();

    List<House> houses = houseRepository.findAll().stream().sorted(Comparator.comparing(House::getName)).toList();
    List<Freezer> freezers =
        freezerRepository.findAll().stream().sorted(Comparator.comparing(Freezer::getName)).toList();

    Map<Integer, Map<Integer, Long>> itemCountsByFreezerAndShelf = buildItemCountsByFreezerAndShelf();
    Map<UUID, List<Freezer>> freezersByHouse = groupFreezersByHouse(freezers);

    List<HouseStatisticsResponse> houseStats = houses.stream()
        .map((house) -> toHouseStatistics(house, freezersByHouse.getOrDefault(house.getId(), List.of()), itemCountsByFreezerAndShelf))
        .toList();

    StatisticsResponse response = new StatisticsResponse();
    response.setHouseCount(houseStats.size());
    response.setHouses(houseStats);
    return response;
  }

  private Map<UUID, List<Freezer>> groupFreezersByHouse(List<Freezer> freezers) {
    Map<UUID, List<Freezer>> freezersByHouse = new HashMap<>();
    for (Freezer freezer : freezers) {
      freezersByHouse.computeIfAbsent(freezer.getHouseId(), key -> new ArrayList<>()).add(freezer);
    }
    return freezersByHouse;
  }

  private Map<Integer, Map<Integer, Long>> buildItemCountsByFreezerAndShelf() {
    Map<Integer, Map<Integer, Long>> itemCounts = new HashMap<>();
    for (ItemRepository.ShelfItemCount row : itemRepository.countItemsByFreezerAndShelf()) {
      itemCounts.computeIfAbsent(row.getFreezerId(), key -> new HashMap<>()).put(row.getShelfNumber(), row.getItemCount());
    }
    return itemCounts;
  }

  private HouseStatisticsResponse toHouseStatistics(
      House house,
      List<Freezer> freezers,
      Map<Integer, Map<Integer, Long>> itemCountsByFreezerAndShelf) {
    HouseStatisticsResponse response = new HouseStatisticsResponse();
    response.setHouseId(house.getId());
    response.setHouseName(house.getName());
    response.setFreezerCount(freezers.size());
    response.setFreezers(freezers.stream().map((freezer) -> toFreezerStatistics(freezer, itemCountsByFreezerAndShelf)).toList());
    return response;
  }

  private FreezerStatisticsResponse toFreezerStatistics(
      Freezer freezer,
      Map<Integer, Map<Integer, Long>> itemCountsByFreezerAndShelf) {
    FreezerStatisticsResponse response = new FreezerStatisticsResponse();
    response.setFreezerId(freezer.getId());
    response.setFreezerName(freezer.getName());
    response.setShelfCount(freezer.getShelfCount());

    Map<Integer, Long> itemCountsByShelf = itemCountsByFreezerAndShelf.getOrDefault(freezer.getId(), Map.of());
    List<ShelfStatisticsResponse> shelfStats = new ArrayList<>();
    for (int shelfNumber = 1; shelfNumber <= freezer.getShelfCount(); shelfNumber += 1) {
      ShelfStatisticsResponse shelf = new ShelfStatisticsResponse();
      shelf.setShelfNumber(shelfNumber);
      shelf.setItemCount(itemCountsByShelf.getOrDefault(shelfNumber, 0L));
      shelfStats.add(shelf);
    }

    response.setShelves(shelfStats);
    return response;
  }
}

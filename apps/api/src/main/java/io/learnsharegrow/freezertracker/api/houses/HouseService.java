package io.learnsharegrow.freezertracker.api.houses;

import io.learnsharegrow.freezertracker.api.auth.AdminGuard;
import io.learnsharegrow.freezertracker.api.common.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseService {
  private final HouseRepository houseRepository;
  private final AdminGuard adminGuard;

  public HouseService(HouseRepository houseRepository, AdminGuard adminGuard) {
    this.houseRepository = houseRepository;
    this.adminGuard = adminGuard;
  }

  @Transactional(readOnly = true)
  public List<HouseResponse> listHouses() {
    adminGuard.requireSuperAdmin();
    return houseRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public HouseResponse getHouse(UUID id) {
    adminGuard.requireSuperAdmin();
    House house = houseRepository.findById(id).orElseThrow(() -> NotFoundException.house(id));
    return toResponse(house);
  }

  @Transactional
  public HouseResponse createHouse(HouseCreateRequest request) {
    adminGuard.requireSuperAdmin();
    House house = new House();
    house.setId(UUID.randomUUID());
    house.setName(request.getName());
    return toResponse(houseRepository.save(house));
  }

  @Transactional
  public HouseResponse updateHouse(UUID id, HouseUpdateRequest request) {
    adminGuard.requireSuperAdmin();
    House house = houseRepository.findById(id).orElseThrow(() -> NotFoundException.house(id));
    house.setName(request.getName());
    return toResponse(houseRepository.save(house));
  }

  @Transactional
  public void deleteHouse(UUID id) {
    adminGuard.requireSuperAdmin();
    House house = houseRepository.findById(id).orElseThrow(() -> NotFoundException.house(id));
    houseRepository.delete(house);
  }

  private HouseResponse toResponse(House house) {
    HouseResponse response = new HouseResponse();
    response.setId(house.getId());
    response.setName(house.getName());
    return response;
  }
}

package io.learnsharegrow.freezertracker.api.freezers;

import io.learnsharegrow.freezertracker.api.common.BadRequestException;
import io.learnsharegrow.freezertracker.api.common.DomainLimitService;
import io.learnsharegrow.freezertracker.api.common.NotFoundException;
import io.learnsharegrow.freezertracker.api.items.ItemRepository;
import io.learnsharegrow.freezertracker.api.security.AccountContext;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FreezerService {
  private final FreezerRepository freezerRepository;
  private final ItemRepository itemRepository;
  private final DomainLimitService domainLimitService;
  private final FreezerMapper mapper = new FreezerMapper();

  public FreezerService(
      FreezerRepository freezerRepository,
      ItemRepository itemRepository,
      DomainLimitService domainLimitService) {
    this.freezerRepository = freezerRepository;
    this.itemRepository = itemRepository;
    this.domainLimitService = domainLimitService;
  }

  @Transactional(readOnly = true)
  public List<FreezerResponse> listFreezers() {
    UUID houseId = AccountContext.requireHouseId();
    return freezerRepository.findAllByHouseId(houseId).stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public FreezerResponse getFreezer(Integer id) {
    UUID houseId = AccountContext.requireHouseId();
    Freezer freezer =
        freezerRepository.findByIdAndHouseId(id, houseId).orElseThrow(() -> NotFoundException.freezer(id));
    return mapper.toResponse(freezer);
  }

  @Transactional
  public FreezerResponse createFreezer(FreezerCreateRequest request) {
    UUID houseId = AccountContext.requireHouseId();
    int maxFreezersPerHouse = domainLimitService.maxFreezersPerHouse();
    if (freezerRepository.countByHouseId(houseId) >= maxFreezersPerHouse) {
      throw new BadRequestException("House cannot have more than " + maxFreezersPerHouse + " freezers");
    }
    Freezer freezer = mapper.toEntity(request);
    freezer.setHouseId(houseId);
    return mapper.toResponse(freezerRepository.save(freezer));
  }

  @Transactional
  public FreezerResponse updateFreezer(Integer id, FreezerUpdateRequest request) {
    UUID houseId = AccountContext.requireHouseId();
    Freezer freezer =
        freezerRepository.findByIdAndHouseId(id, houseId).orElseThrow(() -> NotFoundException.freezer(id));
    String currentName = freezer.getName();
    String requestedName = request.getName();
    if (requestedName != null && !requestedName.equals(currentName)) {
      if (freezerRepository.existsByHouseIdAndNameAndIdNot(houseId, requestedName, id)) {
        throw new BadRequestException("Freezer name already exists");
      }
    }
    mapper.updateEntity(freezer, request);
    return mapper.toResponse(freezerRepository.save(freezer));
  }

  @Transactional
  public void deleteFreezer(Integer id) {
    UUID houseId = AccountContext.requireHouseId();
    Freezer freezer =
        freezerRepository.findByIdAndHouseId(id, houseId).orElseThrow(() -> NotFoundException.freezer(id));
    if (itemRepository.existsByHouseIdAndFreezerId(houseId, id)) {
      throw new BadRequestException("Cannot delete freezer while items are assigned to it");
    }
    freezerRepository.delete(freezer);
  }
}

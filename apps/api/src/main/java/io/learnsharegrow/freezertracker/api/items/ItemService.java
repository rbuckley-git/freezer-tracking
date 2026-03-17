package io.learnsharegrow.freezertracker.api.items;

import io.learnsharegrow.freezertracker.api.common.BadRequestException;
import io.learnsharegrow.freezertracker.api.common.DomainLimitService;
import io.learnsharegrow.freezertracker.api.common.NotFoundException;
import io.learnsharegrow.freezertracker.api.freezers.Freezer;
import io.learnsharegrow.freezertracker.api.freezers.FreezerRepository;
import io.learnsharegrow.freezertracker.api.security.AccountContext;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
  private final ItemRepository itemRepository;
  private final FreezerRepository freezerRepository;
  private final DomainLimitService domainLimitService;
  private final ItemMapper mapper = new ItemMapper();
  private final ItemReferenceService referenceService;

  public ItemService(
      ItemRepository itemRepository,
      FreezerRepository freezerRepository,
      ItemReferenceService referenceService,
      DomainLimitService domainLimitService) {
    this.itemRepository = itemRepository;
    this.freezerRepository = freezerRepository;
    this.referenceService = referenceService;
    this.domainLimitService = domainLimitService;
  }

  @Transactional(readOnly = true)
  public ItemListResponse listItems(int page, int size) {
    UUID houseId = AccountContext.requireHouseId();
    Page<Item> items = itemRepository.findAllByHouseId(houseId, PageRequest.of(page, size));
    var freezerNames = freezerRepository.findAllByHouseId(houseId).stream()
        .collect(java.util.stream.Collectors.toMap(Freezer::getId, Freezer::getName));
    return new ItemListResponse(
        items.map(item -> mapper.toResponse(item, freezerNames.getOrDefault(item.getFreezerId(), "Unknown"))).toList(),
        items.getNumber(),
        items.getSize(),
        items.getTotalElements(),
        items.getTotalPages());
  }

  @Transactional(readOnly = true)
  public ItemResponse getItem(UUID id) {
    UUID houseId = AccountContext.requireHouseId();
    Item item = itemRepository.findByIdAndHouseId(id, houseId).orElseThrow(() -> NotFoundException.item(id));
    Freezer freezer = freezerRepository.findByIdAndHouseId(item.getFreezerId(), houseId)
        .orElseThrow(() -> NotFoundException.freezer(item.getFreezerId()));
    return mapper.toResponse(item, freezer.getName());
  }

  @Transactional
  public ItemResponse createItem(ItemCreateRequest request) {
    UUID houseId = AccountContext.requireHouseId();
    Freezer freezer = ensureFreezerExists(houseId, request.getFreezerId());
    ensureShelfHasCapacity(houseId, request.getFreezerId(), request.getShelfNumber());
    Item item = mapper.toEntity(UUID.randomUUID(), request);
    item.setHouseId(houseId);
    return mapper.toResponse(itemRepository.save(item), freezer.getName());
  }

  @Transactional
  public ItemResponse updateItem(UUID id, ItemUpdateRequest request) {
    UUID houseId = AccountContext.requireHouseId();
    Item item = itemRepository.findByIdAndHouseId(id, houseId).orElseThrow(() -> NotFoundException.item(id));
    Freezer freezer = ensureFreezerExists(houseId, request.getFreezerId());
    boolean destinationChanged = !item.getFreezerId().equals(request.getFreezerId())
        || !item.getShelfNumber().equals(request.getShelfNumber());
    if (destinationChanged) {
      ensureShelfHasCapacity(houseId, request.getFreezerId(), request.getShelfNumber());
    }
    mapper.updateEntity(item, request);
    return mapper.toResponse(itemRepository.save(item), freezer.getName());
  }

  @Transactional
  public void deleteItem(UUID id) {
    UUID houseId = AccountContext.requireHouseId();
    Item item = itemRepository.findByIdAndHouseId(id, houseId).orElseThrow(() -> NotFoundException.item(id));
    itemRepository.delete(item);
  }

  @Transactional(readOnly = true)
  public NextReferenceResponse nextReference() {
    UUID houseId = AccountContext.requireHouseId();
    return referenceService.nextReference(houseId);
  }

  private Freezer ensureFreezerExists(UUID houseId, Integer freezerId) {
    return freezerRepository.findByIdAndHouseId(freezerId, houseId)
        .orElseThrow(() -> BadRequestException.freezerId(freezerId));
  }

  private void ensureShelfHasCapacity(UUID houseId, Integer freezerId, Integer shelfNumber) {
    int maxItemsPerShelf = domainLimitService.maxItemsPerShelf();
    if (itemRepository.countByHouseIdAndFreezerIdAndShelfNumber(houseId, freezerId, shelfNumber)
        >= maxItemsPerShelf) {
      throw new BadRequestException("Shelf cannot contain more than " + maxItemsPerShelf + " items");
    }
  }
}

package io.learnsharegrow.freezertracker.api.items;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemReferenceService {
  private final ItemRepository itemRepository;

  public ItemReferenceService(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @Transactional(readOnly = true)
  public NextReferenceResponse nextReference(UUID houseId) {
    Optional<String> maxReference = itemRepository.findMaxReferenceByHouseId(houseId);
    long nextValue = maxReference.map(Long::parseLong).orElse(0L) + 1;
    String formatted = String.format("%08d", nextValue);
    return new NextReferenceResponse(formatted);
  }
}

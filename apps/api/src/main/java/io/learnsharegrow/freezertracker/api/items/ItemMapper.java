package io.learnsharegrow.freezertracker.api.items;

import java.util.UUID;

public class ItemMapper {
  public Item toEntity(UUID id, ItemCreateRequest request) {
    Item item = new Item();
    item.setId(id);
    item.setReference(request.getReference());
    item.setFreezeDate(request.getFreezeDate());
    item.setBestBefore(request.getBestBefore());
    item.setDescription(request.getDescription());
    item.setFreezerId(request.getFreezerId());
    item.setShelfNumber(request.getShelfNumber());
    item.setWeight(request.getWeight());
    item.setSize(request.getSize());
    return item;
  }

  public void updateEntity(Item item, ItemUpdateRequest request) {
    item.setReference(request.getReference());
    item.setFreezeDate(request.getFreezeDate());
    item.setBestBefore(request.getBestBefore());
    item.setDescription(request.getDescription());
    item.setFreezerId(request.getFreezerId());
    item.setShelfNumber(request.getShelfNumber());
    item.setWeight(request.getWeight());
    item.setSize(request.getSize());
  }

  public ItemResponse toResponse(Item item, String freezerName) {
    ItemResponse response = new ItemResponse();
    response.setId(item.getId());
    response.setReference(item.getReference());
    response.setFreezeDate(item.getFreezeDate());
    response.setBestBefore(item.getBestBefore());
    response.setDescription(item.getDescription());
    response.setFreezerId(item.getFreezerId());
    response.setFreezerName(freezerName);
    response.setShelfNumber(item.getShelfNumber());
    response.setWeight(item.getWeight());
    response.setSize(item.getSize());
    return response;
  }
}

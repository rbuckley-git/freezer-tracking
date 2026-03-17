package io.learnsharegrow.freezertracker.api.items;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
@Validated
public class ItemController {
  private final ItemService itemService;

  public ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @GetMapping
  public ItemListResponse listItems(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return itemService.listItems(page, size);
  }

  @GetMapping("/{id:[0-9a-fA-F-]{36}}")
  public ItemResponse getItem(@PathVariable UUID id) {
    return itemService.getItem(id);
  }

  @GetMapping("/next-reference")
  public NextReferenceResponse nextReference() {
    return itemService.nextReference();
  }

  @PostMapping
  public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemCreateRequest request) {
    ItemResponse response = itemService.createItem(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id:[0-9a-fA-F-]{36}}")
  public ItemResponse updateItem(@PathVariable UUID id, @Valid @RequestBody ItemUpdateRequest request) {
    return itemService.updateItem(id, request);
  }

  @DeleteMapping("/{id:[0-9a-fA-F-]{36}}")
  public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
    itemService.deleteItem(id);
    return ResponseEntity.noContent().build();
  }
}

package io.learnsharegrow.freezertracker.api.houses;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/houses")
public class HouseController {
  private final HouseService houseService;

  public HouseController(HouseService houseService) {
    this.houseService = houseService;
  }

  @GetMapping
  public List<HouseResponse> listHouses() {
    return houseService.listHouses();
  }

  @GetMapping("/{id}")
  public HouseResponse getHouse(@PathVariable UUID id) {
    return houseService.getHouse(id);
  }

  @PostMapping
  public ResponseEntity<HouseResponse> createHouse(@Valid @RequestBody HouseCreateRequest request) {
    HouseResponse response = houseService.createHouse(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  public HouseResponse updateHouse(@PathVariable UUID id, @Valid @RequestBody HouseUpdateRequest request) {
    return houseService.updateHouse(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteHouse(@PathVariable UUID id) {
    houseService.deleteHouse(id);
    return ResponseEntity.noContent().build();
  }
}

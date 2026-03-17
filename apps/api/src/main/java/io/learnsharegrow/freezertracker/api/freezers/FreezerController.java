package io.learnsharegrow.freezertracker.api.freezers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/freezers")
@Validated
public class FreezerController {
  private final FreezerService freezerService;

  public FreezerController(FreezerService freezerService) {
    this.freezerService = freezerService;
  }

  @GetMapping
  public List<FreezerResponse> listFreezers() {
    return freezerService.listFreezers();
  }

  @GetMapping("/{id}")
  public FreezerResponse getFreezer(@PathVariable @Min(1) Integer id) {
    return freezerService.getFreezer(id);
  }

  @PostMapping
  public ResponseEntity<FreezerResponse> createFreezer(
      @Valid @RequestBody FreezerCreateRequest request) {
    FreezerResponse response = freezerService.createFreezer(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  public FreezerResponse updateFreezer(
      @PathVariable @Min(1) Integer id, @Valid @RequestBody FreezerUpdateRequest request) {
    return freezerService.updateFreezer(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFreezer(@PathVariable @Min(1) Integer id) {
    freezerService.deleteFreezer(id);
    return ResponseEntity.noContent().build();
  }
}

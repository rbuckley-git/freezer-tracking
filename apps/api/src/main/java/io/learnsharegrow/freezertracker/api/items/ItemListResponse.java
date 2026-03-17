package io.learnsharegrow.freezertracker.api.items;

import java.util.List;

public class ItemListResponse {
  private List<ItemResponse> items;
  private int page;
  private int size;
  private long totalItems;
  private int totalPages;

  public ItemListResponse() {}

  public ItemListResponse(List<ItemResponse> items, int page, int size, long totalItems, int totalPages) {
    this.items = items;
    this.page = page;
    this.size = size;
    this.totalItems = totalItems;
    this.totalPages = totalPages;
  }

  public List<ItemResponse> getItems() {
    return items;
  }

  public void setItems(List<ItemResponse> items) {
    this.items = items;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public long getTotalItems() {
    return totalItems;
  }

  public void setTotalItems(long totalItems) {
    this.totalItems = totalItems;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }
}

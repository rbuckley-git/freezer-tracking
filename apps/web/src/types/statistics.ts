export type ShelfStatistics = {
  shelfNumber: number;
  itemCount: number;
};

export type FreezerStatistics = {
  freezerId: number;
  freezerName: string;
  shelfCount: number;
  shelves: ShelfStatistics[];
};

export type HouseStatistics = {
  houseId: string;
  houseName: string;
  freezerCount: number;
  freezers: FreezerStatistics[];
};

export type Statistics = {
  houseCount: number;
  houses: HouseStatistics[];
};

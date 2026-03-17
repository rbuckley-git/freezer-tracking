export type Item = {
  id: string;
  reference: string;
  freezeDate: string;
  bestBefore: string;
  description: string;
  freezerId: number;
  freezerName: string;
  shelfNumber: number;
  weight?: string;
  size?: 'S' | 'M' | 'L';
};

export type ItemList = {
  items: Item[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type ItemCreate = Omit<Item, 'id' | 'freezerName'>;
export type ItemUpdate = Omit<Item, 'id' | 'freezerName'>;

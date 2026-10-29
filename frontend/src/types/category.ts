export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  parentId?: number;
  displayOrder?: number;
  active: boolean;
  createdAt?: string;
}

export interface CategoryRequest {
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  parentId?: number;
  displayOrder?: number;
  active?: boolean;
}


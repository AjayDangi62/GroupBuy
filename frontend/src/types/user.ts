export interface UserProfile {
  id: number;
  email: string;
  username: string;
  fullName: string;
  role: 'CUSTOMER' | 'SELLER' | 'ADMIN';
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  createdAt: string;
  updatedAt: string;
}


export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email: string;
  username: string;
  fullName: string;
  role: 'CUSTOMER' | 'SELLER' | 'ADMIN';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  fullName: string;
  role?: 'CUSTOMER' | 'SELLER';
}

export interface RefreshTokenRequest {
  refreshToken: string;
}


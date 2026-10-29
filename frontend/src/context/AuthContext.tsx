import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, USER_INFO_KEY } from '../api/client';
import { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth';
import { UserProfile } from '../types/user';

interface AuthContextType {
  user: UserProfile | null;
  authData: AuthResponse | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isSeller: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  register: (data: RegisterRequest) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authData, setAuthData] = useState<AuthResponse | null>(() => {
    const saved = localStorage.getItem(USER_INFO_KEY);
    return saved ? JSON.parse(saved) : null;
  });
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchProfile = useCallback(async () => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const profile = await userApi.getCurrentUser();
      setUser(profile);
    } catch {
      // If fetching profile fails, user may be unauthorized or token expired
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfile();

    const handleAutoLogout = () => {
      setAuthData(null);
      setUser(null);
    };

    window.addEventListener('auth:logout', handleAutoLogout);
    return () => window.removeEventListener('auth:logout', handleAutoLogout);
  }, [fetchProfile]);

  const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
    setIsLoading(true);
    try {
      const response = await authApi.login(credentials);
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(response));
      setAuthData(response);
      await fetchProfile();
      return response;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    setIsLoading(true);
    try {
      const response = await authApi.register(data);
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(response));
      setAuthData(response);
      await fetchProfile();
      return response;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY) || undefined;
    await authApi.logout(refreshToken);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_INFO_KEY);
    setAuthData(null);
    setUser(null);
  };

  const refreshProfile = async (): Promise<void> => {
    await fetchProfile();
  };

  const role = user?.role || authData?.role;
  const isAuthenticated = !!localStorage.getItem(ACCESS_TOKEN_KEY);
  const isAdmin = role === 'ADMIN';
  const isSeller = role === 'SELLER' || role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        authData,
        isAuthenticated,
        isAdmin,
        isSeller,
        isLoading,
        login,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};


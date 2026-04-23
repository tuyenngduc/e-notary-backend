export type UserRole = 'CLIENT' | 'NOTARY' | 'ADMIN' | string;

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignUpRequest {
  email: string;
  phoneNumber: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  email: string;
  role: UserRole;
}

export interface UserResponse {
  userId?: string;
  email: string;
  role?: UserRole;
  phoneNumber?: string;
  verificationStatus?: 'PENDING' | 'VERIFIED' | 'REJECTED';
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface RefreshResponse {
  token: string;
  refreshToken: string;
  email: string;
}

export interface ApiErrorResponse {
  status?: number;
  errorCode?: string;
  message?: string;
  code?: string;
  errors?: Record<string, string>;
}

export interface AuthSession {
  token: string;
  refreshToken: string;
  email: string;
  role: UserRole;
}


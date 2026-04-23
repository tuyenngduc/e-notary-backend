import { api } from '../../lib/http';
import { getAuthSession } from '../../lib/authStorage';
import type { LoginRequest, LoginResponse, SignUpRequest, UserResponse } from '../../types/auth';

interface ApiEnvelope<T> {
  status: number;
  message: string;
  data: T;
}

export async function loginApi(payload: LoginRequest): Promise<LoginResponse> {
  const response = await api.post<ApiEnvelope<LoginResponse>>('/api/auth/login', payload);
  return response.data.data;
}

export async function registerApi(payload: SignUpRequest): Promise<UserResponse> {
  const response = await api.post<ApiEnvelope<UserResponse>>('/api/users', payload);
  return response.data.data;
}

export async function logoutApi(): Promise<void> {
  const refreshToken = getAuthSession()?.refreshToken;
  await api.post('/api/auth/logout', refreshToken ? { refreshToken } : {});
}

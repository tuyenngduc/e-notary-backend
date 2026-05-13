import { api } from '../../lib/http';
import type { SignUpRequest, UserResponse } from '../../types/auth';

interface ApiEnvelope<T> {
  status: number;
  message: string;
  data: T;
}

interface PagedData<T> {
  content: T[];
}

export async function createNotaryApi(payload: SignUpRequest): Promise<UserResponse> {
  const response = await api.post<ApiEnvelope<UserResponse>>('/api/admin/notaries', payload);
  return response.data.data;
}

export async function listNotariesApi(): Promise<UserResponse[]> {
  const response = await api.get<ApiEnvelope<PagedData<UserResponse>>>('/api/admin/users', {
    params: {
      role: 'NOTARY',
      size: 20,
      page: 0,
    },
  });

  return response.data.data.content;
}


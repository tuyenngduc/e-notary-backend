import { api } from '../../lib/http';
import type { UpdateProfilePayload, UserProfile } from '../../types/profile';

interface ApiEnvelope<T> {
  status: number;
  message: string;
  data: T;
}

export async function getProfileApi(): Promise<UserProfile> {
  const response = await api.get<ApiEnvelope<UserProfile>>('/api/profile');
  return response.data.data;
}

export async function updateProfileApi(payload: UpdateProfilePayload): Promise<UserProfile> {
  const response = await api.put<ApiEnvelope<UserProfile>>('/api/profile', payload);
  return response.data.data;
}

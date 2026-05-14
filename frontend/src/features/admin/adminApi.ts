import { api } from '../../lib/http';
import type { SignUpRequest, UserResponse } from '../../types/auth';
import type { NotaryServiceType, NotaryServiceTypeRequest, ContractTemplate } from '../../types/admin';

export interface DashboardSummary {
  totalUsers: number;
  totalNotaries: number;
  pendingRequests: number;
  completedRequests: number;
}

export interface RevenueData {
  month: string;
  revenue: number;
}

export interface RequestsChartData {
  serviceType: string;
  count: number;
}

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

export async function listUsersApi(): Promise<UserResponse[]> {
  const response = await api.get<ApiEnvelope<PagedData<UserResponse>>>('/api/admin/users', {
    params: {
      size: 50,
      page: 0,
    },
  });

  return response.data.data.content;
}

export async function toggleUserStatusApi(userId: string): Promise<UserResponse> {
  const response = await api.put<ApiEnvelope<UserResponse>>(`/api/admin/users/${userId}/status`);
  return response.data.data;
}

export async function listServicesApi(): Promise<NotaryServiceType[]> {
  const response = await api.get<ApiEnvelope<PagedData<NotaryServiceType>>>('/api/admin/services', {
    params: { size: 100, page: 0 },
  });
  return response.data.data.content;
}

export async function createServiceApi(payload: NotaryServiceTypeRequest): Promise<NotaryServiceType> {
  const response = await api.post<ApiEnvelope<NotaryServiceType>>('/api/admin/services', payload);
  return response.data.data;
}

export async function updateServiceApi(id: string, payload: NotaryServiceTypeRequest): Promise<NotaryServiceType> {
  const response = await api.put<ApiEnvelope<NotaryServiceType>>(`/api/admin/services/${id}`, payload);
  return response.data.data;
}

export async function deleteServiceApi(id: string): Promise<void> {
  await api.delete(`/api/admin/services/${id}`);
}

export async function listTemplatesApi(serviceTypeId?: string): Promise<ContractTemplate[]> {
  const params: Record<string, string> = { onlyActive: 'false' };
  if (serviceTypeId) params.serviceTypeId = serviceTypeId;
  
  const response = await api.get<ApiEnvelope<ContractTemplate[]>>('/api/templates', { params });
  return response.data.data;
}

export async function createTemplateApi(formData: FormData): Promise<ContractTemplate> {
  const response = await api.post<ApiEnvelope<ContractTemplate>>('/api/admin/templates', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data.data;
}

export async function updateTemplateApi(id: string, formData: FormData): Promise<ContractTemplate> {
  const response = await api.put<ApiEnvelope<ContractTemplate>>(`/api/admin/templates/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data.data;
}

export async function deleteTemplateApi(id: string): Promise<void> {
  await api.delete(`/api/admin/templates/${id}`);
}

export async function getDashboardSummaryApi(): Promise<DashboardSummary> {
  const response = await api.get<DashboardSummary>('/api/admin/dashboard/summary');
  return response.data;
}

export async function getDashboardRevenueApi(): Promise<RevenueData[]> {
  const response = await api.get<RevenueData[]>('/api/admin/dashboard/revenue');
  return response.data;
}

export async function getDashboardRequestsChartApi(): Promise<RequestsChartData[]> {
  const response = await api.get<RequestsChartData[]>('/api/admin/dashboard/requests-chart');
  return response.data;
}


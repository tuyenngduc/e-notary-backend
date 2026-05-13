import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from './config';
import { clearAuthSession, getAuthSession, setAuthSession } from './authStorage';
import type { AuthSession, RefreshResponse } from '../types/auth';

export const AUTH_SESSION_CLEARED_EVENT = 'enotary-auth-cleared';

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface ApiEnvelope<T> {
  status: number;
  message: string;
  data: T;
}

const authlessApi = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

let isRefreshing = false;
let refreshPromise: Promise<AuthSession | null> | null = null;

async function refreshAccessToken(): Promise<AuthSession | null> {
  const current = getAuthSession();
  if (!current?.refreshToken) {
    return null;
  }

  const response = await authlessApi.post<ApiEnvelope<RefreshResponse>>('/api/auth/refresh', {
    refreshToken: current.refreshToken,
  });

  const refreshed = response.data.data;

  const nextSession: AuthSession = {
    token: refreshed.token,
    refreshToken: refreshed.refreshToken,
    email: refreshed.email,
    role: current.role,
  };

  setAuthSession(nextSession);
  return nextSession;
}

api.interceptors.request.use((config) => {
  const session = getAuthSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const status = error.response?.status as number | undefined;

    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !String(originalRequest.url ?? '').includes('/api/auth/login') &&
      !String(originalRequest.url ?? '').includes('/api/auth/refresh')
    ) {
      originalRequest._retry = true;

      if (!isRefreshing) {
        isRefreshing = true;
        refreshPromise = refreshAccessToken()
          .catch(() => null)
          .finally(() => {
            isRefreshing = false;
          }) as Promise<AuthSession | null>;
      }

      const refreshed = await refreshPromise;
      if (refreshed?.token) {
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${refreshed.token}`;
        return api(originalRequest);
      }

      clearAuthSession();
      window.dispatchEvent(new Event(AUTH_SESSION_CLEARED_EVENT));
    }

    return Promise.reject(error);
  },
);


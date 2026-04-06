import type { AxiosError } from 'axios';
import type { ApiErrorResponse } from '../types/auth';

export function toApiErrorMessage(error: unknown, fallback = 'Co loi xay ra'): string {
  const axiosError = error as AxiosError<ApiErrorResponse>;

  const serverMessage = axiosError.response?.data?.message;
  if (serverMessage) {
    return serverMessage;
  }

  const fieldErrors = axiosError.response?.data?.errors;
  if (fieldErrors) {
    const first = Object.values(fieldErrors)[0];
    if (first) {
      return first;
    }
  }

  return fallback;
}


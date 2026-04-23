import type { UserRole } from '../types/auth';

const ROLE_ROUTE_MAP: Record<string, string> = {
  ADMIN: '/admin/dashboard',
  NOTARY: '/notary/dashboard',
  CLIENT: '/customer/dashboard',
};

export function getDefaultRouteByRole(role?: UserRole): string {
  return ROLE_ROUTE_MAP[String(role ?? '').toUpperCase()] ?? '/';
}

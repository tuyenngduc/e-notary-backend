import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { clearAuthSession, getAuthSession, setAuthSession } from '../lib/authStorage';
import { loginApi } from '../features/auth/authApi';
import type { AuthSession, LoginRequest, SignUpRequest } from '../types/auth';
import { registerApi } from '../features/auth/authApi';
import { AUTH_SESSION_CLEARED_EVENT } from '../lib/http';

interface AuthContextValue {
  session: AuthSession | null;
  isAuthenticated: boolean;
  login: (payload: LoginRequest) => Promise<AuthSession>;
  register: (payload: SignUpRequest) => Promise<AuthSession>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => getAuthSession());

  useEffect(() => {
    const handleAuthCleared = () => {
      setSession(null);
    };

    window.addEventListener(AUTH_SESSION_CLEARED_EVENT, handleAuthCleared);
    return () => window.removeEventListener(AUTH_SESSION_CLEARED_EVENT, handleAuthCleared);
  }, []);

  const normalizeRole = (role?: string) => {
    if (!role) return 'CLIENT';
    return role.replace('ROLE_', '').toUpperCase();
  };

  const login = async (payload: LoginRequest) => {
    const response = await loginApi(payload);
    const nextSession: AuthSession = {
      token: response.token,
      refreshToken: response.refreshToken,
      email: response.email,
      role: normalizeRole(response.role),
    };
    setSession(nextSession);
    setAuthSession(nextSession);
    return nextSession;
  };

  const register = async (payload: SignUpRequest) => {
    await registerApi(payload);
    return login({ email: payload.email, password: payload.password });
  };

  const logout = () => {
    clearAuthSession();
    setSession(null);
  };

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: !!session?.token,
      login,
      register,
      logout,
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}


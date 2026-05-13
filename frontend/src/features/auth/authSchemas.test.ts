import { describe, expect, it } from 'vitest';
import { loginSchema, registerSchema } from './authSchemas';

describe('authSchemas', () => {
  it('validates login payload', () => {
    const parsed = loginSchema.safeParse({
      email: 'client@example.com',
      password: 'secret123',
    });

    expect(parsed.success).toBe(true);
  });

  it('rejects invalid register payload', () => {
    const parsed = registerSchema.safeParse({
      email: 'invalid',
      phoneNumber: '123456',
      password: '123',
      confirmPassword: '1234',
    });

    expect(parsed.success).toBe(false);
  });
});


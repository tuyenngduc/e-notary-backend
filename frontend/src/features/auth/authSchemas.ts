import { z } from 'zod';

const vietnamPhonePattern = /^(?:\+84|0)[0-9\s\-()]+$/;

export const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
});

export const registerSchema = z
  .object({
    email: z.string().email('Email không hợp lệ'),
    phoneNumber: z
      .string()
      .min(1, 'Số điện thoại không được để trống')
      .regex(vietnamPhonePattern, 'Số điện thoại phải bắt đầu bằng 0 hoặc +84'),
    password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
    confirmPassword: z.string().min(1, 'Vui lòng nhập lại mật khẩu'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Mật khẩu nhập lại không khớp',
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;


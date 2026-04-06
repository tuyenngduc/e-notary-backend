import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { toApiErrorMessage } from '../../../lib/apiError';
import { getDefaultRouteByRole } from '../../../lib/roleRedirect';
import { type RegisterFormValues, registerSchema } from '../authSchemas';
import { AuthLayout } from '../components/AuthLayout';

export function RegisterPage() {
  const { register: registerAccount } = useAuth();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      phoneNumber: '',
      password: '',
      confirmPassword: '',
    },
  });


  const onSubmit = handleSubmit(async (data) => {
    setSubmitError('');
    try {
      const nextSession = await registerAccount({
        email: data.email,
        phoneNumber: data.phoneNumber,
        password: data.password,
      });
      navigate(getDefaultRouteByRole(nextSession.role));
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Đăng ký thất bại'));
    }
  });

  return (
    <AuthLayout
      title="Đăng ký tải khoản công chứng trực tuyến"
      subtitle="Nhanh chóng, an toàn và tiện lợi - tạo tài khoản của bạn ngay hôm nay để trải nghiệm dịch vụ công chứng trực tuyến hàng đầu."
      alternateText="Đã có tài khoản?"
      alternateLabel="Đăng nhập"
      alternateTo="/login"
    >
      <div className="form-header">
        <h2>Đăng ký</h2>
        <p>Tạo tài khoản trong vòng 1 phútcd </p>
      </div>

      <form className="auth-form" onSubmit={onSubmit} noValidate>
        <label className="field">
          <span>Email</span>
          <input type="email" placeholder="Nhập địa chỉ email của bạn" {...register('email')} />
          {errors.email && <small>{errors.email.message}</small>}
        </label>

        <label className="field">
          <span>Số điện thoại</span>
          <input type="tel" placeholder="0987 654 321 hoặc +84 ..." {...register('phoneNumber')} />
          {errors.phoneNumber && <small>{errors.phoneNumber.message}</small>}
        </label>

        <label className="field">
          <span>Mật khẩu</span>
          <input type="password" placeholder="Tối thiểu 6 ký tự" {...register('password')} />
          {errors.password && <small>{errors.password.message}</small>}
        </label>

        <label className="field">
          <span>Nhập lại mật khẩu</span>
          <input type="password" placeholder="Xác nhận mật khẩu của bạn" {...register('confirmPassword')} />
          {errors.confirmPassword && <small>{errors.confirmPassword.message}</small>}
        </label>

        {submitError && <div className="form-error">{submitError}</div>}

        <button type="submit" className="primary-btn" disabled={isSubmitting}>
          {isSubmitting ? 'Đang xử lý...' : 'Tạo tài khoản'}
        </button>
      </form>
    </AuthLayout>
  );
}


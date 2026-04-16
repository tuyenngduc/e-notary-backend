import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { toApiErrorMessage } from '../../../lib/apiError';
import { getDefaultRouteByRole } from '../../../lib/roleRedirect';
import { type LoginFormValues, loginSchema } from '../authSchemas';
import { AuthLayout } from '../components/AuthLayout';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [submitError, setSubmitError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });


  const onSubmit = handleSubmit(async (data) => {
    setSubmitError('');
    try {
      const nextSession = await login(data);
      const redirectPath = (location.state as { from?: string } | null)?.from;
      navigate(redirectPath || getDefaultRouteByRole(nextSession.role));
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Đăng nhập thất bại'));
    }
  });

  return (
    <AuthLayout
      title="Công chứng mọi lúc, ký số mọi nơi"
      subtitle="Giải pháp kết nối bạn với công chứng viên chỉ trong vài phút. An toàn hơn, nhanh chóng hơn và không cần chờ đợi."
      alternateText="Chưa có tài khoản?"
      alternateLabel="Đăng ký ngay"
      alternateTo="/register"
    >
      <div className="form-header">
        <h2>Đăng nhập</h2>
        <p>Truy cập workspace công chứng của bạn</p>
      </div>

      <form className="auth-form" onSubmit={onSubmit} noValidate>
        <label className="field">
          <span>Email</span>
          <input type="email" placeholder="Nhập địa chỉ email của bạn" {...register('email')} />
          {errors.email && <small>{errors.email.message}</small>}
        </label>

        <label className="field">
          <span>Mật khẩu</span>
          <div className="password-field">
            <input
              type={showPassword ? "text" : "password"}
              placeholder="Nhập mật khẩu của bạn"
              {...register('password')}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
            >
              {showPassword ? '👁️' : '👁️‍🗨️'}
            </button>
          </div>
          {errors.password && <small>{errors.password.message}</small>}
        </label>

        {submitError && <div className="form-error">{submitError}</div>}

        <button type="submit" className="primary-btn" disabled={isSubmitting}>
          {isSubmitting ? 'Đang xử lý...' : 'Đăng nhập'}
        </button>
      </form>
    </AuthLayout>
  );
}


import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { toApiErrorMessage } from '../../../lib/apiError';
import { getDefaultRouteByRole } from '../../../lib/roleRedirect';
import { type LoginFormValues, loginSchema } from '../authSchemas';

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
      const redirectPath =
        (location.state as { from?: { pathname?: string } } | null)?.from?.pathname;
      navigate(redirectPath || getDefaultRouteByRole(nextSession.role));
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Đăng nhập thất bại'));
    }
  });

  return (
    <main className="auth-screen">
      <section className="auth-card">
        <div className="card-header">
          <h1>Đăng nhập</h1>
          <p>Đăng nhập để quản lý yêu cầu công chứng và lịch hẹn của bạn.</p>
        </div>

        <form className="form-stack" onSubmit={onSubmit} noValidate>
          <label className="field">
            <span>Email</span>
            <input type="email" placeholder="email@example.com" {...register('email')} />
            {errors.email && <small>{errors.email.message}</small>}
          </label>

          <label className="field">
            <span>Mật khẩu</span>
            <div className="password-wrap">
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                {...register('password')}
              />
              <button
                type="button"
                className="password-eye"
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                onClick={() => setShowPassword((prev) => !prev)}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
            {errors.password && <small>{errors.password.message}</small>}
          </label>

          {submitError && <div className="form-error">{submitError}</div>}

          <button type="submit" className="primary-btn w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Đang xử lý...' : 'Đăng nhập'}
          </button>
        </form>

        <div className="auth-footer-links">
          <p>
            Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
          </p>
          <p>
            <Link to="/">Quay lại trang chủ</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

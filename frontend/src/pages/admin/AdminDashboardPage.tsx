import { useEffect, useState } from 'react';
import { DashboardLayout } from '../../components/DashboardLayout';
import { createNotaryApi, listNotariesApi } from '../../features/admin/adminApi';
import { toApiErrorMessage } from '../../lib/apiError';
import type { UserResponse } from '../../types/auth';

interface NotaryFormState {
  email: string;
  phoneNumber: string;
  password: string;
  confirmPassword: string;
}

const initialFormState: NotaryFormState = {
  email: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
};

export function AdminDashboardPage() {
  const [form, setForm] = useState<NotaryFormState>(initialFormState);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [submitError, setSubmitError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [notaries, setNotaries] = useState<UserResponse[]>([]);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const loadNotaries = async () => {
    setIsLoading(true);
    try {
      const data = await listNotariesApi();
      setNotaries(data);
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Không tải được danh sách công chứng viên'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadNotaries();
  }, []);

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitError('');
    setSuccessMessage('');

    if (form.password !== form.confirmPassword) {
      setSubmitError('Mật khẩu nhập lại không khớp');
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await createNotaryApi({
        email: form.email,
        phoneNumber: form.phoneNumber,
        password: form.password,
      });

      setSuccessMessage(`Đã tạo tài khoản công chứng viên: ${created.email}`);
      setForm(initialFormState);
      await loadNotaries();
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Tạo công chứng viên thất bại'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <DashboardLayout role="admin">
      <div className="page-content">
        <div className="page-header">
          <h1>Trang chủ</h1>
          <p>Quản lý tài khoản công chứng viên do admin tạo.</p>
        </div>

        <section className="soft-card form-card-spaced">
          <h2>Tạo tài khoản công chứng viên</h2>

          <form className="form-stack" onSubmit={onSubmit} noValidate>
            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
                placeholder="notary@example.com"
                required
              />
            </label>

            <label className="field">
              <span>Số điện thoại</span>
              <input
                type="tel"
                value={form.phoneNumber}
                onChange={(event) => setForm((prev) => ({ ...prev, phoneNumber: event.target.value }))}
                placeholder="0987xxxxxx"
                required
              />
            </label>

            <div className="two-cols">
              <label className="field">
                <span>Mật khẩu</span>
                <div className="password-wrap">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={form.password}
                    onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
                    placeholder="Tối thiểu 6 ký tự"
                    required
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
              </label>

              <label className="field">
                <span>Nhập lại mật khẩu</span>
                <div className="password-wrap">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={form.confirmPassword}
                    onChange={(event) => setForm((prev) => ({ ...prev, confirmPassword: event.target.value }))}
                    placeholder="Nhập lại mật khẩu"
                    required
                  />
                  <button
                    type="button"
                    className="password-eye"
                    aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                  >
                    {showConfirmPassword ? '🙈' : '👁️'}
                  </button>
                </div>
              </label>
            </div>

            {submitError ? <div className="form-error">{submitError}</div> : null}
            {successMessage ? <div className="success-box">{successMessage}</div> : null}

            <button type="submit" className="primary-btn" disabled={isSubmitting}>
              {isSubmitting ? 'Đang tạo...' : 'Tạo công chứng viên'}
            </button>
          </form>
        </section>

        <section className="soft-card list-card">
          <h2>Danh sách công chứng viên</h2>
          {isLoading ? <p className="muted-text">Đang tải danh sách...</p> : null}
          {!isLoading && notaries.length === 0 ? (
            <p className="muted-text">Chưa có công chứng viên nào.</p>
          ) : null}

          {!isLoading && notaries.length > 0 ? (
            <div className="list-stack">
              {notaries.map((notary) => (
                <div key={notary.userId ?? notary.email} className="list-row" style={{ alignItems: 'center' }}>
                  <div>
                    <h3>{notary.email}</h3>
                    <p className="muted-text">{notary.phoneNumber || 'Chưa có số điện thoại'}</p>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    {notary.verificationStatus === 'VERIFIED' ? (
                      <span className="status-badge badge-green">Đã xác thực</span>
                    ) : notary.verificationStatus === 'REJECTED' ? (
                      <span className="status-badge badge-red">Bị từ chối</span>
                    ) : (
                      <span className="status-badge badge-yellow">Chưa xác thực</span>
                    )}
                    <span className="status-badge badge-indigo">NOTARY</span>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </section>
      </div>
    </DashboardLayout>
  );
}


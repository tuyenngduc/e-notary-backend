import { useEffect, useState } from 'react';
import { DashboardLayout } from '../../components/DashboardLayout';
import { createNotaryApi, listUsersApi, toggleUserStatusApi } from '../../features/admin/adminApi';
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

export function AdminUsersPage() {
  const [form, setForm] = useState<NotaryFormState>(initialFormState);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [submitError, setSubmitError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [roleFilter, setRoleFilter] = useState<'ALL' | 'CLIENT' | 'NOTARY'>('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      const data = await listUsersApi();
      setUsers(data);
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Không tải được danh sách người dùng'));
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleStatus = async (userId: string) => {
    if (!window.confirm("Bạn có chắc chắn muốn thay đổi trạng thái người dùng này?")) return;
    try {
      await toggleUserStatusApi(userId);
      await loadUsers(); // refresh list
      setSubmitError('');
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Lỗi khi cập nhật trạng thái'));
    }
  };

  useEffect(() => {
    void loadUsers();
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
      await loadUsers();
      
      setTimeout(() => {
        setShowCreateModal(false);
        setSuccessMessage('');
      }, 1500);
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Tạo công chứng viên thất bại'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredUsers = users.filter(u => roleFilter === 'ALL' || u.role === roleFilter);

  return (
    <DashboardLayout role="admin">
      <div className="page-content" style={{ maxWidth: '1200px', margin: '0 auto', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>Quản lý Người dùng</h1>
            <p className="muted-text">Quản lý và cấp quyền tài khoản trên toàn hệ thống.</p>
          </div>
          <button 
            className="primary-btn" 
            onClick={() => setShowCreateModal(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <span>+</span> Tạo CCV
          </button>
        </div>

        {/* Filter Tabs */}
        <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
          <button 
            onClick={() => setRoleFilter('ALL')}
            style={{ padding: '0.5rem 1rem', background: 'none', border: 'none', borderBottom: roleFilter === 'ALL' ? '2px solid var(--primary-color)' : 'none', color: roleFilter === 'ALL' ? 'var(--primary-color)' : 'var(--text-muted)', fontWeight: roleFilter === 'ALL' ? 600 : 400, cursor: 'pointer', transition: 'all 0.2s' }}
          >
            Tất cả
          </button>
          <button 
            onClick={() => setRoleFilter('CLIENT')}
            style={{ padding: '0.5rem 1rem', background: 'none', border: 'none', borderBottom: roleFilter === 'CLIENT' ? '2px solid var(--primary-color)' : 'none', color: roleFilter === 'CLIENT' ? 'var(--primary-color)' : 'var(--text-muted)', fontWeight: roleFilter === 'CLIENT' ? 600 : 400, cursor: 'pointer', transition: 'all 0.2s' }}
          >
            Khách hàng
          </button>
          <button 
            onClick={() => setRoleFilter('NOTARY')}
            style={{ padding: '0.5rem 1rem', background: 'none', border: 'none', borderBottom: roleFilter === 'NOTARY' ? '2px solid var(--primary-color)' : 'none', color: roleFilter === 'NOTARY' ? 'var(--primary-color)' : 'var(--text-muted)', fontWeight: roleFilter === 'NOTARY' ? 600 : 400, cursor: 'pointer', transition: 'all 0.2s' }}
          >
            Công chứng viên
          </button>
        </div>

        {submitError && !showCreateModal && (
          <div className="form-error" style={{ marginBottom: '1.5rem', padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px' }}>
            {submitError}
          </div>
        )}

        {/* Users Table */}
        <div className="soft-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead style={{ backgroundColor: 'rgba(0,0,0,0.02)', borderBottom: '1px solid var(--border-color)' }}>
                <tr>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Email</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Số điện thoại</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Vai trò</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Xác thực</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Trạng thái</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>Đang tải danh sách...</td>
                  </tr>
                ) : filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>Không tìm thấy người dùng nào.</td>
                  </tr>
                ) : (
                  filteredUsers.map((user) => (
                    <tr key={user.userId ?? user.email} style={{ borderBottom: '1px solid var(--border-color)' }}>
                      <td style={{ padding: '1rem' }}>
                        <div style={{ fontWeight: 500 }}>{user.email}</div>
                      </td>
                      <td style={{ padding: '1rem', color: 'var(--text-muted)' }}>
                        {user.phoneNumber || '—'}
                      </td>
                      <td style={{ padding: '1rem' }}>
                        <span className={`status-badge ${user.role === 'ADMIN' ? 'badge-red' : user.role === 'NOTARY' ? 'badge-indigo' : 'badge-gray'}`}>
                          {user.role}
                        </span>
                      </td>
                      <td style={{ padding: '1rem' }}>
                        {user.verificationStatus === 'VERIFIED' ? (
                          <span className="status-badge badge-green">Đã xác thực</span>
                        ) : user.verificationStatus === 'REJECTED' ? (
                          <span className="status-badge badge-red">Bị từ chối</span>
                        ) : (
                          <span className="status-badge badge-yellow">Chưa xác thực</span>
                        )}
                      </td>
                      <td style={{ padding: '1rem' }}>
                        {user.isActive === false ? (
                          <span className="status-badge badge-red">Bị Khóa</span>
                        ) : (
                          <span className="status-badge badge-green">Hoạt Động</span>
                        )}
                      </td>
                      <td style={{ padding: '1rem', textAlign: 'right' }}>
                        {user.role !== 'ADMIN' && (
                          <button 
                            className={user.isActive === false ? 'primary-btn' : 'secondary-btn'} 
                            style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}
                            onClick={() => user.userId && handleToggleStatus(user.userId)}
                          >
                            {user.isActive === false ? 'Mở Khóa' : 'Khóa User'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Create Notary Modal */}
        {showCreateModal && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '1rem', backdropFilter: 'blur(4px)' }}>
            <div className="soft-card" style={{ width: '100%', maxWidth: '500px', padding: '2rem', position: 'relative', animation: 'fadeIn 0.2s ease-out' }}>
              <button 
                type="button"
                onClick={() => { setShowCreateModal(false); setForm(initialFormState); setSubmitError(''); setSuccessMessage(''); }}
                style={{ position: 'absolute', top: '1.5rem', right: '1.5rem', background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: 'var(--text-muted)' }}
              >
                &times;
              </button>
              <h2 style={{ marginBottom: '1.5rem', fontSize: '1.5rem' }}>Tạo công chứng viên</h2>
              
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

                <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                  <button type="button" className="ghost-btn" onClick={() => {setShowCreateModal(false); setForm(initialFormState);}} style={{ flex: 1 }}>
                    Hủy
                  </button>
                  <button type="submit" className="primary-btn" disabled={isSubmitting} style={{ flex: 1 }}>
                    {isSubmitting ? 'Đang tạo...' : 'Tạo tài khoản'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

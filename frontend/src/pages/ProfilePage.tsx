import { useEffect, useState } from 'react';
import { DashboardLayout } from '../components/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { getProfileApi, updateProfileApi } from '../features/profile/profileApi';
import { toApiErrorMessage } from '../lib/apiError';
import type { UpdateProfilePayload, UserProfile } from '../types/profile';

export function ProfilePage() {
  const { session } = useAuth();
  const rawRole = session?.role;
  const role = rawRole === 'CLIENT' ? 'customer' : (rawRole?.toLowerCase() as 'customer' | 'notary' | 'admin');

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [isEditing, setIsEditing] = useState(false);
  const [form, setForm] = useState<UpdateProfilePayload>({
    fullName: '',
    identityNumber: '',
    dateOfBirth: '',
    gender: 'Nam',
    nationality: 'Việt Nam',
    placeOfOrigin: '',
    placeOfResidence: '',
    issueDate: '',
    issuePlace: '',
  });
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const data = await getProfileApi();
        setProfile(data);
        setForm({
          fullName: data.fullName || '',
          identityNumber: data.identityNumber || '',
          dateOfBirth: data.dateOfBirth || '',
          gender: data.gender || 'Nam',
          nationality: data.nationality || 'Việt Nam',
          placeOfOrigin: data.placeOfOrigin || '',
          placeOfResidence: data.placeOfResidence || '',
          issueDate: data.issueDate || '',
          issuePlace: data.issuePlace || '',
        });

        // Remove auto-editing, let user decide when to click update.
      } catch (err) {
        setError(toApiErrorMessage(err, 'Không tải được thông tin cá nhân'));
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError('');
    setSubmitSuccess('');
    setSubmitting(true);

    if (!form.fullName || !form.identityNumber || !form.dateOfBirth || !form.gender || !form.nationality || !form.placeOfOrigin || !form.placeOfResidence || !form.issueDate || !form.issuePlace) {
      setSubmitError('Vui lòng điền đầy đủ các thông tin.');
      setSubmitting(false);
      return;
    }

    try {
      const updated = await updateProfileApi(form);
      setProfile(updated);
      setIsEditing(false);
      setSubmitSuccess('Cập nhật thông tin thành công!');
      setTimeout(() => setSubmitSuccess(''), 3000);
    } catch (err) {
      setSubmitError(toApiErrorMessage(err, 'Lỗi khi cập nhật thông tin'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <DashboardLayout role={role || 'customer'}>
      <div className="page-content narrow-content">
        <div className="page-header">
          <h1>Thông tin cá nhân</h1>
          <p>Quản lý tài khoản và định danh cá nhân của bạn.</p>
        </div>

        {loading ? (
          <p className="muted-text">Đang tải...</p>
        ) : error ? (
          <div className="form-error">{error}</div>
        ) : profile ? (
          <>
            {submitSuccess && <div className="success-box" style={{ marginBottom: '1rem' }}>{submitSuccess}</div>}

            {profile.verificationStatus !== 'VERIFIED' && !isEditing && (
              <div className="inline-warning" style={{ marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h4 style={{ margin: 0, color: '#92400e' }}>Tài khoản chưa được xác thực thông tin cá nhân</h4>
                  <p style={{ margin: '0.25rem 0 0', fontSize: '0.9rem', color: '#b45309' }}>Vui lòng cập nhật đầy đủ hồ sơ cá nhân để được xác thực.</p>
                </div>
                <button className="primary-btn" onClick={() => setIsEditing(true)}>Cập nhật ngay</button>
              </div>
            )}

            <section className="soft-card">
              <div className="card-header-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <h2 style={{ margin: 0 }}>Hồ sơ của tôi</h2>
                  {profile.verificationStatus === 'VERIFIED' ? (
                    <span className="status-badge badge-green">Đã xác thực</span>
                  ) : profile.verificationStatus === 'REJECTED' ? (
                    <span className="status-badge badge-red">Bị từ chối</span>
                  ) : (
                    <span className="status-badge badge-yellow">Chưa xác thực</span>
                  )}
                </div>
                {!isEditing && (
                  <button className="ghost-btn" onClick={() => setIsEditing(true)}>Chỉnh sửa</button>
                )}
              </div>

              {!isEditing ? (
                <div className="list-stack">
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Email</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.email}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Họ và tên</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.fullName || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Số CCCD</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.identityNumber || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Số điện thoại</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.phoneNumber || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Ngày sinh</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.dateOfBirth || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Giới tính</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.gender || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Quốc tịch</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.nationality || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Quê quán</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.placeOfOrigin || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Nơi thường trú</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.placeOfResidence || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Ngày cấp</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.issueDate || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                  <div className="list-row">
                    <div>
                      <p className="muted-text">Nơi cấp</p>
                      <h3 style={{ fontSize: '1rem', marginTop: '0.25rem' }}>{profile.issuePlace || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Chưa cập nhật</span>}</h3>
                    </div>
                  </div>
                </div>
              ) : (
                <form className="form-stack" onSubmit={handleSubmit}>
                  <div className="field">
                    <span>Email</span>
                    <input type="text" value={profile.email} disabled style={{ backgroundColor: '#f1f5f9', color: '#64748b' }} />
                  </div>

                  <div className="field">
                    <span>Họ và tên đầy đủ *</span>
                    <input
                      type="text"
                      value={form.fullName}
                      onChange={e => setForm({ ...form, fullName: e.target.value })}
                      placeholder="VD: NGUYEN VAN A"
                      required
                    />
                  </div>

                  <div className="field">
                    <span>Số Căn cước công dân *</span>
                    <input
                      type="text"
                      value={form.identityNumber}
                      onChange={e => setForm({ ...form, identityNumber: e.target.value })}
                      placeholder="12 chữ số"
                      required
                      maxLength={12}
                    />
                  </div>

                  <div className="field">
                    <span>Ngày sinh *</span>
                    <input
                      type="date"
                      value={form.dateOfBirth}
                      onChange={e => setForm({ ...form, dateOfBirth: e.target.value })}
                      required
                    />
                  </div>

                  <div className="field">
                    <span>Giới tính *</span>
                    <select value={form.gender} onChange={e => setForm({ ...form, gender: e.target.value })} required>
                      <option value="Nam">Nam</option>
                      <option value="Nữ">Nữ</option>
                      <option value="Khác">Khác</option>
                    </select>
                  </div>

                  <div className="field">
                    <span>Quốc tịch *</span>
                    <input
                      type="text"
                      value={form.nationality}
                      onChange={e => setForm({ ...form, nationality: e.target.value })}
                      required
                    />
                  </div>

                  <div className="field">
                    <span>Quê quán *</span>
                    <textarea
                      rows={2}
                      value={form.placeOfOrigin}
                      onChange={e => setForm({ ...form, placeOfOrigin: e.target.value })}
                      placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
                      required
                    />
                  </div>

                  <div className="field">
                    <span>Nơi thường trú *</span>
                    <textarea
                      rows={2}
                      value={form.placeOfResidence}
                      onChange={e => setForm({ ...form, placeOfResidence: e.target.value })}
                      placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
                      required
                    />
                  </div>

                  <div className="field">
                    <span>Ngày cấp *</span>
                    <input
                      type="date"
                      value={form.issueDate}
                      onChange={e => setForm({ ...form, issueDate: e.target.value })}
                      required
                    />
                  </div>

                  <div className="field">
                    <span>Nơi cấp *</span>
                    <input
                      type="text"
                      value={form.issuePlace}
                      onChange={e => setForm({ ...form, issuePlace: e.target.value })}
                      required
                    />
                  </div>

                  {submitError && <div className="form-error">{submitError}</div>}

                  <div className="action-row" style={{ marginTop: '1rem' }}>
                    <button type="submit" className="primary-btn" disabled={submitting}>
                      {submitting ? 'Đang lưu...' : 'Lưu thông tin'}
                    </button>
                    <button type="button" className="ghost-btn" onClick={() => {
                      setIsEditing(false);
                      setForm({
                        fullName: profile.fullName || '',
                        identityNumber: profile.identityNumber || '',
                        dateOfBirth: profile.dateOfBirth || '',
                        gender: profile.gender || 'Nam',
                        nationality: profile.nationality || 'Việt Nam',
                        placeOfOrigin: profile.placeOfOrigin || '',
                        placeOfResidence: profile.placeOfResidence || '',
                        issueDate: profile.issueDate || '',
                        issuePlace: profile.issuePlace || '',
                      });
                    }} disabled={submitting}>
                      Hủy bỏ
                    </button>
                  </div>
                </form>
              )}
            </section>
          </>
        ) : null}
      </div>
    </DashboardLayout>
  );
}

import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function DashboardPage() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <main className="dashboard-page">
      <section className="dashboard-card">
        <div>
          <p className="dashboard-label">Đã đăng nhập thành công</p>
          <h1>Workspace công chứng điện tử</h1>
          <p className="dashboard-subtitle">
            Chào mừng {session?.email}. Role hiện tại: <strong>{session?.role}</strong>.
          </p>
          <p className="dashboard-subtitle">
            Bước tiếp theo: tạo trang dashboard theo role CLIENT / NOTARY / ADMIN.
          </p>
        </div>

        <button type="button" className="ghost-btn" onClick={handleLogout}>
          Đăng xuất
        </button>
      </section>
    </main>
  );
}


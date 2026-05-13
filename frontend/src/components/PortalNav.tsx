import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface PortalNavProps {
  role: 'customer' | 'notary' | 'admin';
}

export function PortalNav({ role }: PortalNavProps) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const dashboardPath =
    role === 'customer' ? '/customer/dashboard' : role === 'notary' ? '/notary/dashboard' : '/admin/dashboard';

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <nav className="top-nav">
      <div className="container nav-inner">
        <Link to="/" className="brand">
          Công Chứng Điện Tử
        </Link>

        <div className="nav-links">
          <Link to={dashboardPath} className="link-btn">
            Bảng điều khiển
          </Link>
          {role === 'customer' ? (
            <Link to="/customer/new-request" className="link-btn primary-link-btn">
              Yêu cầu mới
            </Link>
          ) : null}
          <button type="button" className="link-btn" onClick={handleLogout}>
            Đăng xuất
          </button>
        </div>
      </div>
    </nav>
  );
}

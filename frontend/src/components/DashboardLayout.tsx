import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import React, { useEffect, useState } from 'react';
import { getProfileApi } from '../features/profile/profileApi';
import type { UserProfile } from '../types/profile';

interface DashboardLayoutProps {
  children: React.ReactNode;
  role: 'customer' | 'notary' | 'admin';
}

export function DashboardLayout({ children, role }: DashboardLayoutProps) {
  const { logout, session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [profile, setProfile] = useState<UserProfile | null>(null);

  useEffect(() => {
    if (session?.token) {
      getProfileApi()
        .then(data => setProfile(data))
        .catch(() => {});
    }
  }, [session?.token]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const customerLinks = [
    { name: 'Trang chủ', path: '/customer/dashboard', icon: <HomeIcon /> },
    { name: 'Thông tin cá nhân', path: '/profile', icon: <UserIcon /> },
    { name: 'Yêu cầu công chứng', path: '/customer/requests', icon: <PlusIcon /> },
  ];

  const notaryLinks = [
    { name: 'Trang chủ', path: '/notary/dashboard', icon: <HomeIcon /> },
    { name: 'Thông tin cá nhân', path: '/profile', icon: <UserIcon /> },
    { name: 'Yêu cầu công chứng', path: '/notary/requests', icon: <ListIcon /> },
    { name: 'Lịch hẹn', path: '/notary/appointments', icon: <CalendarIcon /> },
  ];

  const adminLinks = [
    { name: 'Trang chủ', path: '/admin/dashboard', icon: <HomeIcon /> },
    { name: 'Thông tin cá nhân', path: '/profile', icon: <UserIcon /> },
  ];

  const links =
    role === 'customer'
      ? customerLinks
      : role === 'notary'
      ? notaryLinks
      : adminLinks;

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <Link to="/" className="brand" style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
            <span>E-Notary</span>
            <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-muted)' }}>
              {role === 'customer' ? 'Cổng người dân' : role === 'notary' ? 'Cổng công chứng viên' : 'Cổng quản trị viên'}
            </span>
          </Link>
        </div>
        <nav className="sidebar-nav">
          {links.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={`sidebar-link ${location.pathname === link.path ? 'active' : ''}`}
            >
              {link.icon}
              {link.name}
            </Link>
          ))}
        </nav>
        <div className="sidebar-footer" style={{ padding: '0.75rem' }}>
          <button type="button" className="ghost-btn w-full" onClick={handleLogout} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'flex-start', color: '#ef4444', fontSize: '0.95rem', padding: '0.4rem 0.75rem', fontWeight: 500 }}>
            <LogoutIcon width={18} height={18} />
            Đăng xuất
          </button>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span style={{ fontWeight: 500, color: 'var(--text-muted)' }}>
              Xin chào, {profile?.verificationStatus === 'VERIFIED' && profile?.fullName ? profile.fullName : (session?.email || 'Người dùng')}
            </span>
          </div>
        </header>
        {role === 'notary' && profile?.verificationStatus !== 'VERIFIED' ? (
          <div style={{ padding: '0 2rem' }}>
            <div className="inline-warning" style={{ marginTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
              <div>
                <b>Tài khoản công chứng viên chưa được xác thực.</b>
                <div style={{ fontSize: '0.9rem', marginTop: '0.25rem' }}>
                  Vui lòng cập nhật đầy đủ hồ sơ để có thể tiếp nhận / xử lý yêu cầu.
                </div>
              </div>
              <Link className="primary-btn" to="/profile" style={{ whiteSpace: 'nowrap' }}>
                Xác thực ngay
              </Link>
            </div>
          </div>
        ) : null}
        <main>
          {children}
        </main>
      </div>
    </div>
  );
}

// Simple inline SVG icons
function HomeIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
      <polyline points="9 22 9 12 15 12 15 22"></polyline>
    </svg>
  );
}

function PlusIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <line x1="12" y1="5" x2="12" y2="19"></line>
      <line x1="5" y1="12" x2="19" y2="12"></line>
    </svg>
  );
}

function LogoutIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" {...props}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
      <polyline points="16 17 21 12 16 7"></polyline>
      <line x1="21" y1="12" x2="9" y2="12"></line>
    </svg>
  );
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
      <circle cx="12" cy="7" r="4"></circle>
    </svg>
  );
}

function ListIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <line x1="8" y1="6" x2="21" y2="6"></line>
      <line x1="8" y1="12" x2="21" y2="12"></line>
      <line x1="8" y1="18" x2="21" y2="18"></line>
      <circle cx="4" cy="6" r="1"></circle>
      <circle cx="4" cy="12" r="1"></circle>
      <circle cx="4" cy="18" r="1"></circle>
    </svg>
  );
}

function CalendarIcon() {
  return (
    <svg viewBox="0 0 24 24">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
      <line x1="16" y1="2" x2="16" y2="6"></line>
      <line x1="8" y1="2" x2="8" y2="6"></line>
      <line x1="3" y1="10" x2="21" y2="10"></line>
    </svg>
  );
}


import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDefaultRouteByRole } from '../lib/roleRedirect';

const features = [
  {
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 3l7 4v5c0 5-3.4 8-7 9-3.6-1-7-4-7-9V7l7-4z" />
      </svg>
    ),
    title: 'An Toàn',
    description: 'Mã hóa end-to-end và xác thực hai yếu tố bảo vệ tài liệu của bạn',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M13 2L4 14h6l-1 8 9-12h-6l1-8z" />
      </svg>
    ),
    title: 'Nhanh Chóng',
    description: 'Xử lý yêu cầu chỉ trong vài phút với lịch hẹn linh hoạt',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 3l8 4-8 4-8-4 8-4z" />
        <path d="M6 11v4c0 2 2.7 3.5 6 3.5s6-1.5 6-3.5v-4" />
      </svg>
    ),
    title: 'Chuyên Nghiệp',
    description: 'Công chứng viên được xác minh và có kinh nghiệm',
  },
];

export function HomePage() {
  const { isAuthenticated, session } = useAuth();

  return (
    <main className="landing-page">
      <nav className="top-nav">
        <div className="container nav-inner">
          <div className="brand">Công Chứng Điện Tử</div>
          <div className="nav-links">
            {isAuthenticated ? (
              <Link className="link-btn primary-link-btn" to={getDefaultRouteByRole(session?.role)}>
                Vào dashboard
              </Link>
            ) : (
              <>
                <Link className="link-btn" to="/login">
                  Đăng nhập
                </Link>
                <Link className="link-btn primary-link-btn" to="/register">
                  Đăng ký
                </Link>
              </>
            )}
          </div>
        </div>
      </nav>

      <section className="container hero-section">
        <h1>Công chứng điện tử an toàn và tiện lợi</h1>
        <p>
          Nền tảng giúp bạn gửi hồ sơ, theo dõi tiến độ và làm việc với công chứng viên trực tuyến một cách nhanh chóng,
          minh bạch.
        </p>
        <div className="hero-actions">
          <Link className="primary-btn" to={isAuthenticated ? getDefaultRouteByRole(session?.role) : '/register'}>
            Bắt đầu ngay
          </Link>
          <Link className="ghost-btn" to="/login">
            Đăng nhập hệ thống
          </Link>
        </div>
      </section>

      <section className="container feature-grid-section">
        <h2>Tại sao chọn chúng tôi</h2>
        <div className="feature-grid">
          {features.map((feature) => (
            <article className="feature-card" key={feature.title}>
              <div className="feature-icon" aria-hidden="true">
                {feature.icon}
              </div>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}

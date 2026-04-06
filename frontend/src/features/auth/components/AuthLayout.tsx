import { Link } from 'react-router-dom';

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  alternateText: string;
  alternateLabel: string;
  alternateTo: string;
  children: React.ReactNode;
}

export function AuthLayout({
  title,
  subtitle,
  alternateText,
  alternateLabel,
  alternateTo,
  children,
}: AuthLayoutProps) {
  return (
    <main className="auth-page">
      <section className="auth-panel auth-panel-brand">
        <div className="brand-tag">E-Notary Platform</div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
        <div className="brand-list">
          <div>Thủ tục trong tầm tay: Nộp hồ sơ, đặt lịch hẹn và nhận bản sao điện tử ngay trên một nền tảng duy nhất.</div>
          <div>Xác thực diện mạo trực diện: Thực hiện các phiên đối soát định danh (eKYC) trực tuyến qua kênh truyền dữ liệu mã hóa, đảm bảo tính pháp lý và an toàn tuyệt đối.</div>
          <div>Bằng chứng số bất biến: Ứng dụng công nghệ Blockchain đảm bảo tính toàn vẹn và khả năng chống giả mạo vĩnh viễn.</div>
        </div>
      </section>

      <section className="auth-panel auth-panel-form">
        {children}
        <div className="auth-switch">
          {alternateText}{' '}
          <Link to={alternateTo}>
            {alternateLabel}
          </Link>
        </div>
      </section>
    </main>
  );
}


import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardLayout } from '../../components/DashboardLayout';
import {
  listAcceptedNotaryRequestsApi,
  listNotaryRequestsApi,
} from '../../features/requests/requestApi';
import { toContractTypeLabel, toRequestStatusMeta } from '../../lib/enumLabels';
import { toApiErrorMessage } from '../../lib/apiError';
import type { NotaryRequest } from '../../types/request';

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN');
}

export function NotaryDashboardPage() {
  const [processingRequests, setProcessingRequests] = useState<NotaryRequest[]>([]);
  const [acceptedRequests, setAcceptedRequests] = useState<NotaryRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [processingData, acceptedData] = await Promise.all([
          listNotaryRequestsApi('PROCESSING'),
          listAcceptedNotaryRequestsApi(),
        ]);
        setProcessingRequests(processingData);
        setAcceptedRequests(acceptedData);
      } catch (loadError) {
        setError(toApiErrorMessage(loadError, 'Không tải được dashboard công chứng viên'));
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, []);

  const stats = useMemo(() => {
    const scheduled = acceptedRequests.filter((item) => item.status === 'SCHEDULED').length;
    const completed = acceptedRequests.filter((item) => item.status === 'COMPLETED').length;

    return {
      pending: processingRequests.length,
      accepted: acceptedRequests.length,
      scheduled,
      completed,
    };
  }, [acceptedRequests, processingRequests.length]);

  const processingPreview = processingRequests.slice(0, 5);
  const acceptedPreview = acceptedRequests.slice(0, 5);

  return (
    <DashboardLayout role="notary">
      <div className="page-content">
        <div className="page-header">
          <h1>Trang chủ</h1>
          <p>Theo dõi yêu cầu đang xử lý và hồ sơ đã tiếp nhận.</p>
        </div>

        <section className="stats-grid">
          <article className="soft-card stat-card">
            <p>Chờ tiếp nhận</p>
            <h3>{stats.pending}</h3>
          </article>
          <article className="soft-card stat-card">
            <p>Đã tiếp nhận</p>
            <h3>{stats.accepted}</h3>
          </article>
          <article className="soft-card stat-card">
            <p>Đã lên lịch</p>
            <h3>{stats.scheduled}</h3>
          </article>
          <article className="soft-card stat-card">
            <p>Hoàn thành</p>
            <h3>{stats.completed}</h3>
          </article>
        </section>

        <section className="soft-card list-card">
          <div className="page-header with-action" style={{ padding: 0, marginBottom: '1rem' }}>
            <div>
              <h2 style={{ margin: 0 }}>Hàng đợi & hồ sơ gần đây</h2>
              <p className="muted-text" style={{ marginTop: '0.25rem' }}>Xử lý nhanh các yêu cầu mới và theo dõi hồ sơ bạn đã tiếp nhận.</p>
            </div>
            <Link className="link-btn" to="/notary/requests">Xem tất cả</Link>
          </div>

          {loading ? <p className="muted-text">Đang tải dữ liệu...</p> : null}
          {error ? <div className="form-error">{error}</div> : null}

          {!loading && !error ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1rem' }}>
              <div className="soft-card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <h3 style={{ margin: 0, fontSize: '1.05rem' }}>Chờ tiếp nhận</h3>
                  <Link className="ghost-btn" to="/notary/requests?tab=processing" style={{ padding: '0.4rem 0.7rem' }}>Mở</Link>
                </div>
                {processingPreview.length === 0 ? (
                  <p className="muted-text">Không có yêu cầu mới.</p>
                ) : (
                  <div className="list-stack">
                    {processingPreview.map((request) => {
                      const statusInfo = toRequestStatusMeta(request.status);
                      return (
                        <Link key={request.requestId} className="list-row" to={`/notary/request/${request.requestId}`}>
                          <div>
                            <h4 style={{ margin: 0 }}>{toContractTypeLabel(request.contractType)}</h4>
                            <p className="muted-text">Tạo: {formatDate(request.createdAt)}</p>
                          </div>
                          <span className={`status-badge ${statusInfo.tone}`}>{statusInfo.label}</span>
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>

              <div className="soft-card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <h3 style={{ margin: 0, fontSize: '1.05rem' }}>Hồ sơ của tôi</h3>
                  <Link className="ghost-btn" to="/notary/requests?tab=accepted" style={{ padding: '0.4rem 0.7rem' }}>Mở</Link>
                </div>
                {acceptedPreview.length === 0 ? (
                  <p className="muted-text">Bạn chưa tiếp nhận hồ sơ nào.</p>
                ) : (
                  <div className="list-stack">
                    {acceptedPreview.map((request) => {
                      const statusInfo = toRequestStatusMeta(request.status);
                      return (
                        <Link key={request.requestId} className="list-row" to={`/notary/request/${request.requestId}`}>
                          <div>
                            <h4 style={{ margin: 0 }}>{toContractTypeLabel(request.contractType)}</h4>
                            <p className="muted-text">Cập nhật: {formatDate(request.updatedAt)}</p>
                          </div>
                          <span className={`status-badge ${statusInfo.tone}`}>{statusInfo.label}</span>
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </DashboardLayout>
  );
}


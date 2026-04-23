import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { DashboardLayout } from '../../components/DashboardLayout';
import {
  downloadDocumentApi,
  getRequestApi,
  getRequestDocumentsApi,
  uploadRequestDocumentApi,
  replaceDocumentApi,
  cancelRequestApi,
} from '../../features/requests/requestApi';
import { toApiErrorMessage } from '../../lib/apiError';
import type { DocType, DocumentItem, NotaryRequest } from '../../types/request';

const statusMeta: Record<string, { label: string; tone: string }> = {
  NEW: { label: 'Mới tạo', tone: 'badge-gray' },
  PROCESSING: { label: 'Đang xử lý', tone: 'badge-blue' },
  ACCEPTED: { label: 'Đã tiếp nhận', tone: 'badge-indigo' },
  SCHEDULED: { label: 'Đã lên lịch', tone: 'badge-purple' },
  AWAITING_PAYMENT: { label: 'Chờ thanh toán', tone: 'badge-yellow' },
  COMPLETED: { label: 'Hoàn thành', tone: 'badge-green' },
  CANCELLED: { label: 'Đã hủy', tone: 'badge-gray' },
  REJECTED: { label: 'Bị từ chối', tone: 'badge-red' },
};

const contractTypeLabels: Record<string, string> = {
  TRANSFER_OF_PROPERTY: 'Hợp đồng chuyển nhượng',
  POWER_OF_ATTORNEY: 'Ủy quyền',
  LOAN_AGREEMENT: 'Hợp đồng vay mượn',
  WILL: 'Di chúc',
  MARRIAGE_CONTRACT: 'Hợp đồng hôn nhân',
  BUSINESS_CONTRACT: 'Hợp đồng thương mại',
  OTHER: 'Loại khác',
};

const serviceTypeLabels: Record<string, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
};

const docTypeOptions: Array<{ value: DocType; label: string }> = [
  { value: 'ID_CARD', label: 'Giấy tờ tùy thân' },
  { value: 'PROPERTY_PAPER', label: 'Giấy tờ tài sản' },
  { value: 'DRAFT_CONTRACT', label: 'Bản dự thảo hợp đồng' },
  { value: 'SIGNED_DOCUMENT', label: 'Tài liệu đã ký' },
  { value: 'SESSION_VIDEO', label: 'Video phiên họp' },
];

const docTypeLabels: Record<string, string> = docTypeOptions.reduce((acc, curr) => {
  acc[curr.value] = curr.label;
  return acc;
}, {} as Record<string, string>);

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN');
}

export function CustomerRequestDetailPage() {
  const { id = '' } = useParams();
  const [request, setRequest] = useState<NotaryRequest | null>(null);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);
  const [viewingDocUrl, setViewingDocUrl] = useState<string | null>(null);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [requestData, docsData] = await Promise.all([
          getRequestApi(id),
          getRequestDocumentsApi(id),
        ]);
        setRequest(requestData);
        setDocuments(docsData);
      } catch (loadError) {
        setError(toApiErrorMessage(loadError, 'Không tải được thông tin yêu cầu'));
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      void load();
    }
  }, [id]);

  const statusInfo = useMemo(() => {
    if (!request) return null;
    return statusMeta[request.status] ?? { label: request.status, tone: 'badge-gray' };
  }, [request]);

  const handleDownload = async (item: DocumentItem) => {
    try {
      const blob = await downloadDocumentApi(item.documentId);
      const objectUrl = URL.createObjectURL(blob);
      const anchor = window.document.createElement('a');
      anchor.href = objectUrl;
      anchor.download = item.filePath.split('/').pop() || `${item.documentId}.bin`;
      window.document.body.append(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } catch (downloadError) {
      setError(toApiErrorMessage(downloadError, 'Không tải được tài liệu'));
    }
  };

  const handleView = async (item: DocumentItem) => {
    try {
      const blob = await downloadDocumentApi(item.documentId);
      let finalBlob = blob;
      if (item.filePath.toLowerCase().endsWith('.pdf') && blob.type !== 'application/pdf') {
        finalBlob = new Blob([blob], { type: 'application/pdf' });
      } else if ((item.filePath.toLowerCase().endsWith('.jpg') || item.filePath.toLowerCase().endsWith('.jpeg')) && blob.type !== 'image/jpeg') {
        finalBlob = new Blob([blob], { type: 'image/jpeg' });
      } else if (item.filePath.toLowerCase().endsWith('.png') && blob.type !== 'image/png') {
        finalBlob = new Blob([blob], { type: 'image/png' });
      }
      
      const objectUrl = URL.createObjectURL(finalBlob);
      setViewingDocUrl(objectUrl);
    } catch (err) {
      setError(toApiErrorMessage(err, 'Không xem được tài liệu'));
    }
  };

  const handleUploadForType = async (file: File, docType: DocType) => {
    if (!id) return;
    setUploading(true);
    setError('');
    try {
      const doc = await uploadRequestDocumentApi(id, file, docType);
      setDocuments(prev => [...prev, doc]);
    } catch (err) {
      setError(toApiErrorMessage(err, `Lỗi tải lên ${docTypeLabels[docType]}`));
    } finally {
      setUploading(false);
    }
  };

  const handleReplace = async (file: File, documentId: string) => {
    setUploading(true);
    setError('');
    try {
      const updatedDoc = await replaceDocumentApi(documentId, file);
      setDocuments(prev => prev.map(d => d.documentId === documentId ? updatedDoc : d));
    } catch (err) {
      setError(toApiErrorMessage(err, 'Lỗi khi thay thế tài liệu'));
    } finally {
      setUploading(false);
    }
  };

  const handleCancelClick = () => {
    setShowCancelConfirm(true);
  };

  const executeCancel = async () => {
    if (!id || !request) return;
    
    setLoading(true);
    setError('');
    try {
      const updated = await cancelRequestApi(id);
      setRequest(updated);
    } catch (err) {
      setError(toApiErrorMessage(err, 'Lỗi khi hủy yêu cầu'));
    } finally {
      setLoading(false);
    }
  };

  const isTerminalStatus = request?.status === 'COMPLETED' || request?.status === 'CANCELLED' || request?.status === 'REJECTED';

  return (
    <DashboardLayout role="customer">
      <div className="page-content">
        {loading ? <p className="muted-text">Đang tải dữ liệu...</p> : null}
        {error ? <div className="form-error">{error}</div> : null}

        {!loading && !error && request ? (
          <>
            <div className="page-header with-action">
              <div>
                <h1>Chi tiết yêu cầu</h1>
                <p>
                  {contractTypeLabels[request.contractType] || request.contractType} - {serviceTypeLabels[request.serviceType] || request.serviceType}
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                {statusInfo ? <span className={`status-badge ${statusInfo.tone}`}>{statusInfo.label}</span> : null}
                {!isTerminalStatus && (
                  <button type="button" className="ghost-btn" style={{ color: '#ef4444' }} onClick={handleCancelClick}>
                    Hủy yêu cầu
                  </button>
                )}
              </div>
            </div>

            <div className="content-grid">
              <div className="grid-left" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', gridColumn: '1 / -1' }}>
                <section className="soft-card">
                  <h2>Thông tin chung</h2>
                  <div className="list-stack">
                    <div className="list-row">
                      <div><p className="muted-text">Mã yêu cầu</p><h4>{request.requestId.split('-')[0]}</h4></div>
                    </div>
                    <div className="list-row">
                      <div><p className="muted-text">Ngày tạo</p><h4>{formatDate(request.createdAt)}</h4></div>
                    </div>
                    <div className="list-row">
                      <div><p className="muted-text">Trạng thái</p><h4 style={{ color: 'var(--primary)' }}>{statusInfo?.label}</h4></div>
                    </div>
                    <div className="list-row">
                      <div><p className="muted-text">Mô tả</p><h4>{request.description || 'Không có mô tả'}</h4></div>
                    </div>
                  </div>

                  {request.rejectionReason ? (
                    <div className="inline-warning" style={{ marginTop: '1rem' }}>Lý do từ chối: {request.rejectionReason}</div>
                  ) : null}

                  {request.meetingUrl ? (
                    <div style={{ marginTop: '1rem' }}>
                      <a className="primary-btn inline-btn" href={request.meetingUrl} target="_blank" rel="noreferrer">
                        Tham gia phòng họp trực tuyến
                      </a>
                    </div>
                  ) : null}
                </section>

                <section>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h2 style={{ margin: 0 }}>Hồ sơ tài liệu</h2>
                    {uploading && <span className="muted-text" style={{ fontSize: '0.9rem' }}>Đang tải lên...</span>}
                  </div>
                  <p className="muted-text" style={{ marginBottom: '1.5rem' }}>Vui lòng bổ sung đầy đủ các giấy tờ còn thiếu để hồ sơ sớm được thẩm định.</p>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
                    {(request.documentRequirements?.requiredDocTypes || []).map(docType => {
                      const docsForType = documents.filter(d => d.docType === docType);
                      return (
                        <div key={docType} className="soft-card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem', border: docsForType.length === 0 ? '1px dashed #cbd5e1' : undefined }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ fontSize: '1rem', margin: 0 }}>{docTypeLabels[docType] || docType}</h3>
                            <span className={`status-badge ${docsForType.length > 0 ? 'badge-green' : 'badge-yellow'}`}>
                              {docsForType.length > 0 ? `${docsForType.length} tệp` : 'Còn thiếu'}
                            </span>
                          </div>

                          {docsForType.length > 0 ? (
                            <div className="list-stack" style={{ gap: '0.5rem' }}>
                              {docsForType.map(item => (
                                <div key={item.documentId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(0,0,0,0.02)', padding: '0.5rem 0.75rem', borderRadius: '6px' }}>
                                  <span style={{ fontSize: '0.85rem', wordBreak: 'break-all', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>
                                    {item.filePath.split('/').pop()}
                                  </span>
                                  <div style={{ display: 'flex', gap: '0.25rem', alignItems: 'center' }}>
                                    <button type="button" className="ghost-btn" style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem', minWidth: 'auto', color: 'var(--primary)' }} onClick={() => handleView(item)}>Xem</button>
                                    <button type="button" className="ghost-btn" style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem', minWidth: 'auto' }} onClick={() => handleDownload(item)}>Tải về</button>
                                    {!isTerminalStatus && (
                                      <label className="ghost-btn" style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem', minWidth: 'auto', color: '#f59e0b', cursor: uploading ? 'wait' : 'pointer', margin: 0, fontWeight: 500 }}>
                                        Cập nhật
                                        <input type="file" style={{ display: 'none' }} disabled={uploading} onChange={(e) => {
                                          const file = e.target.files?.[0];
                                          if (file) handleReplace(file, item.documentId);
                                          e.target.value = '';
                                        }} />
                                      </label>
                                    )}
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div style={{ padding: '1.5rem 0', textAlign: 'center', background: '#f8fafc', borderRadius: '8px' }}>
                              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginBottom: '0.5rem' }}><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="12" y1="18" x2="12" y2="12"></line><line x1="9" y1="15" x2="15" y2="15"></line></svg>
                              <p className="muted-text" style={{ fontSize: '0.85rem', margin: 0 }}>Chưa có tài liệu</p>
                            </div>
                          )}

                          <div style={{ marginTop: 'auto', textAlign: 'center' }}>
                            {isTerminalStatus ? (
                              <p className="muted-text" style={{ fontSize: '0.8rem', margin: 0 }}>Không thể cập nhật tài liệu</p>
                            ) : (
                              <label className="primary-btn ghost-btn" style={{ width: '100%', cursor: uploading ? 'wait' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                                <input
                                  type="file"
                                  style={{ display: 'none' }}
                                  disabled={uploading}
                                  onChange={(e) => {
                                    const file = e.target.files?.[0];
                                    if (file) {
                                      handleUploadForType(file, docType);
                                    }
                                    e.target.value = '';
                                  }}
                                />
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                                Thêm {docTypeLabels[docType]?.toLowerCase() || docType}
                              </label>
                            )}
                          </div>
                        </div>
                      );
                    })}

                    {/* Render extra docs that might have been uploaded but are not strictly required */}
                    {documents.filter(d => !request.documentRequirements?.requiredDocTypes.includes(d.docType)).map(extraDoc => {
                      const docType = extraDoc.docType;
                      // Just group them by docType, we can cheat a little if they are uploaded multiple times
                      if (documents.findIndex(d => d.docType === docType) !== documents.indexOf(extraDoc)) return null;

                      const docsForType = documents.filter(d => d.docType === docType);
                      return (
                        <div key={docType} className="soft-card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ fontSize: '1rem', margin: 0 }}>{docTypeLabels[docType] || docType} (Bổ sung)</h3>
                            <span className="status-badge badge-green">
                              {docsForType.length} tệp
                            </span>
                          </div>

                          <div className="list-stack" style={{ gap: '0.5rem' }}>
                            {docsForType.map(item => (
                              <div key={item.documentId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(0,0,0,0.02)', padding: '0.5rem 0.75rem', borderRadius: '6px' }}>
                                <span style={{ fontSize: '0.85rem', wordBreak: 'break-all', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>
                                  {item.filePath.split('/').pop()}
                                </span>
                                <div style={{ display: 'flex', gap: '0.25rem', alignItems: 'center' }}>
                                  <button type="button" className="ghost-btn" style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem', minWidth: 'auto', color: 'var(--primary)' }} onClick={() => handleView(item)}>Xem</button>
                                  <button type="button" className="ghost-btn" style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem', minWidth: 'auto' }} onClick={() => handleDownload(item)}>Tải về</button>
                                  {!isTerminalStatus && (
                                    <label className="ghost-btn" style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem', minWidth: 'auto', color: '#f59e0b', cursor: uploading ? 'wait' : 'pointer', margin: 0, fontWeight: 500 }}>
                                      Cập nhật
                                      <input type="file" style={{ display: 'none' }} disabled={uploading} onChange={(e) => {
                                        const file = e.target.files?.[0];
                                        if (file) handleReplace(file, item.documentId);
                                        e.target.value = '';
                                      }} />
                                    </label>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </section>
              </div>
            </div>
          </>
        ) : null}

        {viewingDocUrl && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: '#333', zIndex: 9999, display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 1rem', background: '#1e293b' }}>
              <span style={{ color: 'white', fontWeight: 500 }}>Trình xem tài liệu trực tuyến</span>
              <button className="primary-btn" style={{ background: '#ef4444', padding: '0.4rem 1.5rem' }} onClick={() => { URL.revokeObjectURL(viewingDocUrl); setViewingDocUrl(null); }}>
                Đóng (X)
              </button>
            </div>
            <iframe src={viewingDocUrl} style={{ width: '100%', flex: 1, border: 'none', background: '#e2e8f0' }} title="Trình xem tài liệu" />
          </div>
        )}

        {showCancelConfirm && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.6)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
            <div className="soft-card" style={{ width: '100%', maxWidth: '400px', padding: '2rem', textAlign: 'center' }}>
              <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#fee2e2', color: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem auto' }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
              </div>
              <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.25rem' }}>Hủy yêu cầu công chứng</h3>
              <p className="muted-text" style={{ marginBottom: '2rem' }}>Bạn có chắc chắn muốn hủy yêu cầu này không? Hành động này <b>không thể hoàn tác</b>.</p>
              
              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="button" className="ghost-btn" style={{ flex: 1 }} onClick={() => setShowCancelConfirm(false)}>Quay lại</button>
                <button type="button" className="primary-btn" style={{ flex: 1, background: '#ef4444', borderColor: '#ef4444', color: 'white' }} onClick={() => {
                  setShowCancelConfirm(false);
                  executeCancel();
                }}>Xác nhận Hủy</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

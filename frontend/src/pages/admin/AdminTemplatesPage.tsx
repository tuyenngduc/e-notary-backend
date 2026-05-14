import { useEffect, useState, useRef } from 'react';
import { DashboardLayout } from '../../components/DashboardLayout';
import { listTemplatesApi, createTemplateApi, updateTemplateApi, deleteTemplateApi, listServicesApi } from '../../features/admin/adminApi';
import { toApiErrorMessage } from '../../lib/apiError';
import type { ContractTemplate, NotaryServiceType } from '../../types/admin';

export function AdminTemplatesPage() {
  const [templates, setTemplates] = useState<ContractTemplate[]>([]);
  const [services, setServices] = useState<NotaryServiceType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  
  const [serviceTypeId, setServiceTypeId] = useState('');
  const [name, setName] = useState('');
  const [version, setVersion] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [file, setFile] = useState<File | null>(null);
  
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [pageError, setPageError] = useState('');

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [tplData, svcData] = await Promise.all([
        listTemplatesApi(),
        listServicesApi()
      ]);
      setTemplates(tplData);
      setServices(svcData);
      setPageError('');
    } catch (error) {
      setPageError(toApiErrorMessage(error, 'Không tải được dữ liệu'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const openCreateModal = () => {
    setEditingId(null);
    setServiceTypeId(services.length > 0 ? services[0].id : '');
    setName('');
    setVersion('');
    setIsActive(true);
    setFile(null);
    setErrorMsg('');
    setShowModal(true);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const openEditModal = (template: ContractTemplate) => {
    setEditingId(template.id);
    setServiceTypeId(template.serviceTypeId);
    setName(template.name);
    setVersion(template.version || '');
    setIsActive(template.isActive);
    setFile(null);
    setErrorMsg('');
    setShowModal(true);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa/vô hiệu hóa mẫu hợp đồng này?")) return;
    try {
      await deleteTemplateApi(id);
      await loadData();
      setPageError('');
    } catch (error) {
      setPageError(toApiErrorMessage(error, 'Lỗi khi xóa mẫu hợp đồng'));
    }
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    
    if (!editingId && !file) {
      setErrorMsg('Vui lòng chọn file đính kèm');
      return;
    }

    setIsSubmitting(true);
    
    try {
      const formData = new FormData();
      if (name) formData.append('name', name);
      if (version) formData.append('version', version);
      formData.append('isActive', String(isActive));
      if (file) formData.append('file', file);

      if (editingId) {
        await updateTemplateApi(editingId, formData);
      } else {
        formData.append('serviceTypeId', serviceTypeId);
        await createTemplateApi(formData);
      }
      setShowModal(false);
      await loadData();
    } catch (error) {
      setErrorMsg(toApiErrorMessage(error, 'Lưu thông tin thất bại'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const downloadUrl = (id: string) => `http://localhost:8080/api/templates/${id}/download`;

  return (
    <DashboardLayout role="admin">
      <div className="page-content" style={{ maxWidth: '1200px', margin: '0 auto', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>Mẫu Hợp đồng & Biểu mẫu</h1>
            <p className="muted-text">Quản lý các file mẫu chuẩn để người dân tải về.</p>
          </div>
          <button 
            className="primary-btn" 
            onClick={openCreateModal}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <span>+</span> Tải lên Mẫu mới
          </button>
        </div>

        {pageError && (
          <div className="form-error" style={{ marginBottom: '1.5rem', padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px' }}>
            {pageError}
          </div>
        )}

        {/* Templates Table */}
        <div className="soft-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead style={{ backgroundColor: 'rgba(0,0,0,0.02)', borderBottom: '1px solid var(--border-color)' }}>
                <tr>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Tên mẫu</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Loại dịch vụ</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Phiên bản</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)' }}>Trạng thái</th>
                  <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={5} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>Đang tải danh sách...</td>
                  </tr>
                ) : templates.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>Chưa có mẫu văn bản nào.</td>
                  </tr>
                ) : (
                  templates.map((tpl) => {
                    const svcName = services.find(s => s.id === tpl.serviceTypeId)?.name || tpl.serviceTypeCode;
                    return (
                      <tr key={tpl.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '1rem', fontWeight: 500 }}>
                          <a href={downloadUrl(tpl.id)} target="_blank" rel="noreferrer" style={{ color: 'var(--primary-color)', textDecoration: 'none' }}>
                            📄 {tpl.name}
                          </a>
                        </td>
                        <td style={{ padding: '1rem' }}>{svcName}</td>
                        <td style={{ padding: '1rem', color: 'var(--text-muted)' }}>
                          {tpl.version || 'Mặc định'}
                        </td>
                        <td style={{ padding: '1rem' }}>
                          {tpl.isActive ? (
                            <span className="status-badge badge-green">Hoạt động</span>
                          ) : (
                            <span className="status-badge badge-gray">Bị ẩn</span>
                          )}
                        </td>
                        <td style={{ padding: '1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                          <button 
                            className="ghost-btn" 
                            style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem', marginRight: '0.5rem', color: 'var(--primary-color)' }}
                            onClick={() => openEditModal(tpl)}
                          >
                            Sửa
                          </button>
                          <button 
                            className="ghost-btn" 
                            style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}
                            onClick={() => handleDelete(tpl.id)}
                          >
                            Xóa
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Modal */}
        {showModal && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '1rem', backdropFilter: 'blur(4px)' }}>
            <div className="soft-card" style={{ width: '100%', maxWidth: '500px', padding: '2rem', position: 'relative', animation: 'fadeIn 0.2s ease-out' }}>
              <button 
                type="button"
                onClick={() => setShowModal(false)}
                style={{ position: 'absolute', top: '1.5rem', right: '1.5rem', background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: 'var(--text-muted)' }}
              >
                &times;
              </button>
              <h2 style={{ marginBottom: '1.5rem', fontSize: '1.5rem' }}>
                {editingId ? 'Cập nhật Mẫu hợp đồng' : 'Thêm Mẫu Mới'}
              </h2>
              
              <form className="form-stack" onSubmit={onSubmit} noValidate>
                {!editingId && (
                  <label className="field">
                    <span>Loại dịch vụ</span>
                    <select
                      value={serviceTypeId}
                      onChange={(e) => setServiceTypeId(e.target.value)}
                      required
                    >
                      {services.filter(s => s.isActive).map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.serviceCode})</option>
                      ))}
                    </select>
                  </label>
                )}

                <label className="field">
                  <span>Tên mẫu văn bản</span>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="VD: Mẫu hợp đồng ủy quyền xe máy"
                    required
                  />
                </label>

                <label className="field">
                  <span>Phiên bản (Tùy chọn)</span>
                  <input
                    type="text"
                    value={version}
                    onChange={(e) => setVersion(e.target.value)}
                    placeholder="VD: v1.0, 2026"
                  />
                </label>

                <label className="field">
                  <span>File đính kèm (PDF/Word) {!editingId && <span style={{color: 'red'}}>*</span>}</span>
                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={(e) => setFile(e.target.files ? e.target.files[0] : null)}
                    accept=".pdf,.doc,.docx"
                    required={!editingId}
                  />
                  {editingId && <div style={{fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem'}}>Bỏ trống nếu không muốn thay đổi file hiện tại.</div>}
                </label>

                <label className="field checkbox-field" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', marginTop: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={isActive}
                    onChange={(e) => setIsActive(e.target.checked)}
                    style={{ width: 'auto' }}
                  />
                  <span>Áp dụng mẫu này (Đang hoạt động)</span>
                </label>

                {errorMsg && <div className="form-error">{errorMsg}</div>}

                <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                  <button type="button" className="ghost-btn" onClick={() => setShowModal(false)} style={{ flex: 1 }}>
                    Hủy
                  </button>
                  <button type="submit" className="primary-btn" disabled={isSubmitting} style={{ flex: 1 }}>
                    {isSubmitting ? 'Đang lưu...' : 'Lưu lại'}
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

import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DashboardLayout } from '../../components/DashboardLayout';
import {
  createRequestApi,
} from '../../features/requests/requestApi';
import { toApiErrorMessage } from '../../lib/apiError';
import type { ContractType, ServiceType } from '../../types/request';

const contractOptions: Array<{ value: ContractType; label: string; price: string }> = [
  { value: 'TRANSFER_OF_PROPERTY', label: 'Hợp đồng chuyển nhượng', price: '150,000' },
  { value: 'POWER_OF_ATTORNEY', label: 'Ủy quyền', price: '100,000' },
  { value: 'LOAN_AGREEMENT', label: 'Hợp đồng vay mượn', price: '120,000' },
  { value: 'WILL', label: 'Di chúc', price: '200,000' },
  { value: 'MARRIAGE_CONTRACT', label: 'Hợp đồng hôn nhân', price: '180,000' },
  { value: 'BUSINESS_CONTRACT', label: 'Hợp đồng thương mại', price: '160,000' },
  { value: 'OTHER', label: 'Loại khác', price: 'Theo báo giá' },
];

export function NewRequestPage() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [form, setForm] = useState<{
    serviceType: ServiceType;
    contractType: ContractType;
    description: string;
  }>({
    serviceType: 'ONLINE',
    contractType: 'TRANSFER_OF_PROPERTY',
    description: '',
  });

  const selectedContract = useMemo(
    () => contractOptions.find((item) => item.value === form.contractType),
    [form.contractType],
  );

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitError('');

    setSubmitting(true);
    try {
      const created = await createRequestApi({
        serviceType: form.serviceType,
        contractType: form.contractType,
        description: form.description,
      });

      navigate(`/customer/request/${created.requestId}`);
    } catch (error) {
      setSubmitError(toApiErrorMessage(error, 'Không thể tạo yêu cầu'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <DashboardLayout role="customer">
      <div className="page-content narrow-content">
        <div className="page-header">
          <h1>Yêu cầu công chứng mới</h1>
          <p>Điền thông tin để tạo yêu cầu công chứng trực tuyến hoặc trực tiếp.</p>
        </div>

        <section className="soft-card">
          <form className="form-stack" onSubmit={handleSubmit}>
            <label className="field">
              <span>Loại dịch vụ</span>
              <select
                value={form.serviceType}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, serviceType: event.target.value as ServiceType }))
                }
              >
                <option value="ONLINE">Online</option>
                <option value="OFFLINE">Offline</option>
              </select>
            </label>

            <label className="field">
              <span>Loại hợp đồng</span>
              <select
                value={form.contractType}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, contractType: event.target.value as ContractType }))
                }
              >
                {contractOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Mô tả chi tiết</span>
              <textarea
                rows={4}
                value={form.description}
                onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
                placeholder="Nhập thông tin bổ sung cho công chứng viên"
              />
            </label>

            {selectedContract ? (
              <div className="price-panel">
                <strong>Giá dự kiến:</strong> {selectedContract.price} VND
              </div>
            ) : null}

            {submitError ? <div className="form-error">{submitError}</div> : null}

            <div className="action-row">
              <button className="primary-btn" type="submit" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Tạo yêu cầu'}
              </button>
              <button className="ghost-btn" type="button" onClick={() => navigate(-1)} disabled={submitting}>
                Hủy
              </button>
            </div>
          </form>
        </section>
      </div>
    </DashboardLayout>
  );
}


import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

interface Payment {
  id: string;
  requestId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED';
  method?: string;
  transactionId?: string;
  createdAt: string;
  paidAt?: string;
}

export function PaymentPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const requestId = searchParams.get('requestId');
  const [payments, setPayments] = useState<Payment[]>([
    {
      id: 'pay-001',
      requestId: requestId || 'req-001',
      amount: 500000,
      currency: 'VND',
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    },
  ]);

  const [processingPaymentId, setProcessingPaymentId] = useState<string | null>(null);
  const [selectedMethod, setSelectedMethod] = useState<'card' | 'bank' | 'wallet'>('card');
  const [showModal, setShowModal] = useState(false);
  const [selectedPayment, setSelectedPayment] = useState<Payment | null>(null);

  const handlePayment = async (payment: Payment) => {
    setSelectedPayment(payment);
    setShowModal(true);
  };

  const processPayment = async () => {
    if (!selectedPayment) return;

    setProcessingPaymentId(selectedPayment.id);
    try {
      await new Promise(resolve => setTimeout(resolve, 2000));

      setPayments(prev =>
        prev.map(p =>
          p.id === selectedPayment.id
            ? {
                ...p,
                status: 'PAID',
                method: selectedMethod,
                transactionId: `TXN-${Date.now()}`,
                paidAt: new Date().toISOString(),
              }
            : p
        )
      );

      alert(`✓ Thanh toán thành công!\n\nSố tiền: ${selectedPayment.amount.toLocaleString('vi-VN')} ${selectedPayment.currency}\nPhương thức: ${getMethodLabel(selectedMethod)}`);
      setShowModal(false);
      setSelectedPayment(null);
    } catch (err) {
      alert('❌ Thanh toán thất bại. Vui lòng thử lại.');
    } finally {
      setProcessingPaymentId(null);
    }
  };

  const getMethodLabel = (method: 'card' | 'bank' | 'wallet') => {
    const labels = {
      card: 'Thẻ tín dụng/Ghi nợ',
      bank: 'Chuyển khoản ngân hàng',
      wallet: 'Ví điện tử',
    };
    return labels[method];
  };

  const getStatusBadge = (status: Payment['status']) => {
    const statusMap = {
      PENDING: { label: 'Chưa thanh toán', color: '#ff9800' },
      PAID: { label: 'Đã thanh toán', color: '#4caf50' },
      CANCELLED: { label: 'Đã hủy', color: '#f44336' },
      REFUNDED: { label: 'Đã hoàn tiền', color: '#2196f3' },
    };
    return statusMap[status];
  };

  return (
    <div className="payment-page" style={{ maxWidth: '1000px', margin: '0 auto', padding: '2rem' }}>
      <header style={{ marginBottom: '2rem' }}>
        <h1>Quản lý thanh toán</h1>
        <p style={{ color: '#666' }}>Lịch sử và chi tiết các khoản thanh toán dịch vụ công chứng</p>
      </header>

      {payments.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem', background: '#f5f5f5', borderRadius: '8px' }}>
          <p style={{ color: '#999', marginBottom: '1rem' }}>Không có khoản thanh toán nào</p>
          <button className="primary-btn" onClick={() => navigate('/customer/requests')}>
            Xem yêu cầu công chứng
          </button>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
          gap: '1.5rem',
        }}>
          {payments.map(payment => {
            const badge = getStatusBadge(payment.status);
            return (
              <div
                key={payment.id}
                style={{
                  border: '1px solid #e0e0e0',
                  borderRadius: '8px',
                  padding: '1.5rem',
                  background: 'white',
                  boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                  <div>
                    <div style={{ fontSize: '0.9rem', color: '#666' }}>Yêu cầu #</div>
                    <div style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>{payment.requestId}</div>
                  </div>
                  <div
                    style={{
                      padding: '0.5rem 1rem',
                      background: badge.color + '20',
                      color: badge.color,
                      borderRadius: '4px',
                      fontSize: '0.9rem',
                      fontWeight: '500',
                    }}
                  >
                    {badge.label}
                  </div>
                </div>

                <div style={{
                  padding: '1rem 0',
                  borderTop: '1px solid #eee',
                  borderBottom: '1px solid #eee',
                  marginBottom: '1rem',
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span>Số tiền:</span>
                    <span style={{ fontWeight: 'bold', fontSize: '1.2rem', color: '#333' }}>
                      {payment.amount.toLocaleString('vi-VN')} {payment.currency}
                    </span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: '#666' }}>
                    <span>Tạo lúc:</span>
                    <span>{new Date(payment.createdAt).toLocaleString('vi-VN')}</span>
                  </div>
                  {payment.status === 'PAID' && payment.paidAt && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: '#666' }}>
                      <span>Thanh toán lúc:</span>
                      <span>{new Date(payment.paidAt).toLocaleString('vi-VN')}</span>
                    </div>
                  )}
                  {payment.transactionId && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: '#666' }}>
                      <span>Mã giao dịch:</span>
                      <span style={{ fontFamily: 'monospace' }}>{payment.transactionId}</span>
                    </div>
                  )}
                </div>

                {payment.status === 'PENDING' ? (
                  <button
                    className="primary-btn"
                    onClick={() => handlePayment(payment)}
                    style={{ width: '100%' }}
                    disabled={!!processingPaymentId}
                  >
                    💳 Thanh toán ngay
                  </button>
                ) : payment.status === 'PAID' ? (
                  <button
                    className="secondary-btn"
                    onClick={() => {
                      alert(`✓ Thanh toán thành công!\n\nNội dung: Công chứng hợp đồng\nSố tiền: ${payment.amount.toLocaleString('vi-VN')} ${payment.currency}\nMã giao dịch: ${payment.transactionId}\n\nVui lòng lưu giữ hóa đơn này.`);
                    }}
                    style={{ width: '100%' }}
                  >
                    🧾 Xem hóa đơn
                  </button>
                ) : (
                  <button
                    className="ghost-btn"
                    onClick={() => {
                      alert(`Yêu cầu được hủy hoặc hoàn tiền. Liên hệ hỗ trợ để biết chi tiết.`);
                    }}
                    style={{ width: '100%' }}
                    disabled
                  >
                    {payment.status === 'CANCELLED' ? 'Đã hủy' : 'Đã hoàn tiền'}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Payment Modal */}
      {showModal && selectedPayment ? (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          zIndex: 10000,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <div style={{
            background: 'white',
            borderRadius: '12px',
            padding: '2rem',
            maxWidth: '500px',
            width: '90%',
            boxShadow: '0 8px 32px rgba(0,0,0,0.2)',
          }}>
            <h2 style={{ marginTop: 0, marginBottom: '1.5rem' }}>Thanh toán hóa đơn</h2>

            <div style={{
              background: '#f9f9f9',
              padding: '1.5rem',
              borderRadius: '8px',
              marginBottom: '1.5rem',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <span style={{ color: '#666' }}>Yêu cầu công chứng</span>
                <span style={{ fontWeight: 'bold' }}>{selectedPayment.requestId}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <span style={{ color: '#666' }}>Chi phí dịch vụ</span>
                <span>200,000 VND</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <span style={{ color: '#666' }}>Phí giao dịch</span>
                <span>5,000 VND</span>
              </div>
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                paddingTop: '1rem',
                borderTop: '2px solid #e0e0e0',
                fontSize: '1.2rem',
                fontWeight: 'bold',
              }}>
                <span>Tổng cộng</span>
                <span style={{ color: '#4caf50' }}>{selectedPayment.amount.toLocaleString('vi-VN')} VND</span>
              </div>
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', marginBottom: '1rem', fontWeight: '500' }}>
                Chọn phương thức thanh toán
              </label>
              <div style={{ display: 'grid', gap: '0.75rem' }}>
                {(['card', 'bank', 'wallet'] as const).map(method => (
                  <label key={method} style={{
                    display: 'flex',
                    alignItems: 'center',
                    padding: '1rem',
                    border: selectedMethod === method ? '2px solid #4caf50' : '1px solid #e0e0e0',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    background: selectedMethod === method ? '#f0f7f0' : 'white',
                  }}>
                    <input
                      type="radio"
                      name="payment-method"
                      value={method}
                      checked={selectedMethod === method}
                      onChange={() => setSelectedMethod(method)}
                      style={{ marginRight: '0.75rem' }}
                    />
                    <span>
                      {method === 'card' && '💳 Thẻ tín dụng/Ghi nợ'}
                      {method === 'bank' && '🏦 Chuyển khoản ngân hàng'}
                      {method === 'wallet' && '💰 Ví điện tử (Momo, ZaloPay)'}
                    </span>
                  </label>
                ))}
              </div>
            </div>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <button
                className="ghost-btn"
                onClick={() => {
                  setShowModal(false);
                  setSelectedPayment(null);
                }}
                style={{ flex: 1 }}
                disabled={!!processingPaymentId}
              >
                Hủy
              </button>
              <button
                className="primary-btn"
                onClick={processPayment}
                style={{ flex: 1 }}
                disabled={!!processingPaymentId}
              >
                {processingPaymentId ? 'Đang xử lý...' : `💳 Thanh toán ${selectedPayment.amount.toLocaleString('vi-VN')} VND`}
              </button>
            </div>

            <p style={{
              marginTop: '1.5rem',
              fontSize: '0.85rem',
              color: '#999',
              textAlign: 'center',
            }}>
              💡 Thông tin thanh toán được bảo mật. Chúng tôi không lưu trữ chi tiết thẻ.
            </p>
          </div>
        </div>
      ) : null}
    </div>
  );
}


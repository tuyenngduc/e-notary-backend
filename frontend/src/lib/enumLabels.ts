import type { ContractType, DocType, RequestStatus, ServiceType } from '../types/request';

export const serviceTypeLabels: Record<string, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
};

export const contractTypeLabels: Record<string, string> = {
  TRANSFER_OF_PROPERTY: 'Hợp đồng chuyển nhượng',
  POWER_OF_ATTORNEY: 'Ủy quyền',
  LOAN_AGREEMENT: 'Hợp đồng vay mượn',
  WILL: 'Di chúc',
  MARRIAGE_CONTRACT: 'Hợp đồng hôn nhân',
  BUSINESS_CONTRACT: 'Hợp đồng thương mại',
  OTHER: 'Loại khác',
};

export const docTypeLabels: Record<string, string> = {
  ID_CARD: 'Giấy tờ tùy thân',
  PROPERTY_PAPER: 'Giấy tờ tài sản',
  DRAFT_CONTRACT: 'Bản dự thảo hợp đồng',
  SIGNED_DOCUMENT: 'Tài liệu đã ký',
  SESSION_VIDEO: 'Video phiên họp',
};

export const requestStatusLabels: Record<string, { label: string; tone: string }> = {
  NEW: { label: 'Mới tạo', tone: 'badge-gray' },
  PROCESSING: { label: 'Chờ tiếp nhận', tone: 'badge-yellow' },
  ACCEPTED: { label: 'Đã tiếp nhận', tone: 'badge-indigo' },
  SCHEDULED: { label: 'Đã lên lịch', tone: 'badge-purple' },
  AWAITING_PAYMENT: { label: 'Chờ thanh toán', tone: 'badge-yellow' },
  COMPLETED: { label: 'Hoàn thành', tone: 'badge-green' },
  CANCELLED: { label: 'Đã hủy', tone: 'badge-gray' },
  REJECTED: { label: 'Bị từ chối', tone: 'badge-red' },
};

export function toContractTypeLabel(value: ContractType | string | null | undefined) {
  if (!value) return '';
  return contractTypeLabels[value] ?? value;
}

export function toServiceTypeLabel(value: ServiceType | string | null | undefined) {
  if (!value) return '';
  return serviceTypeLabels[value] ?? value;
}

export function toDocTypeLabel(value: DocType | string | null | undefined) {
  if (!value) return '';
  return docTypeLabels[value] ?? value;
}

export function toRequestStatusMeta(value: RequestStatus | string | null | undefined) {
  if (!value) return { label: '', tone: 'badge-gray' };
  return requestStatusLabels[value] ?? { label: String(value), tone: 'badge-gray' };
}


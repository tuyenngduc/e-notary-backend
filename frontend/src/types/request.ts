export type RequestStatus =
  | 'NEW'
  | 'PROCESSING'
  | 'ACCEPTED'
  | 'SCHEDULED'
  | 'AWAITING_PAYMENT'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED'
  | string;

export type ServiceType = 'ONLINE' | 'OFFLINE';

export type ContractType =
  | 'TRANSFER_OF_PROPERTY'
  | 'POWER_OF_ATTORNEY'
  | 'LOAN_AGREEMENT'
  | 'WILL'
  | 'MARRIAGE_CONTRACT'
  | 'BUSINESS_CONTRACT'
  | 'OTHER';

export type DocType = 'ID_CARD' | 'PROPERTY_PAPER' | 'DRAFT_CONTRACT' | 'SIGNED_DOCUMENT' | 'SESSION_VIDEO';

export interface DocumentRequirementResponse {
  requiredDocTypes: DocType[];
  uploadedDocTypes: DocType[];
  missingDocTypes: DocType[];
  readyForAccept: boolean;
}

export interface NotaryRequest {
  requestId: string;
  clientId: string | null;
  notaryId: string | null;
  serviceType: ServiceType;
  contractType: ContractType;
  description: string;
  status: RequestStatus;
  rejectionReason: string | null;
  meetingUrl: string | null;
  createdAt: string;
  updatedAt: string;
  documentIds: string[];
  documentRequirements?: DocumentRequirementResponse;
}

export interface DocumentItem {
  documentId: string;
  requestId: string;
  filePath: string;
  absolutePath: string | null;
  docType: DocType;
  fileHash: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRequestPayload {
  serviceType: ServiceType;
  contractType: ContractType;
  description: string;
}

export interface PagedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type AppointmentStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export interface Appointment {
  appointmentId: string;
  requestId: string;
  serviceType: ServiceType;
  contractType: ContractType;
  scheduledTime: string;
  meetingUrl: string | null;
  physicalAddress: string | null;
  status: AppointmentStatus;
  createdAt: string;
  clientName: string | null;
}

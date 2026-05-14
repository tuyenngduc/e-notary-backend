export interface NotaryServiceType {
  id: string;
  serviceCode: string;
  name: string;
  basePrice: number;
  description: string;
  isActive: boolean;
}

export interface NotaryServiceTypeRequest {
  serviceCode: string;
  name: string;
  basePrice: number;
  description: string;
  isActive: boolean;
}

export interface ContractTemplate {
  id: string;
  serviceTypeId: string;
  serviceTypeCode: string;
  name: string;
  fileUrl: string;
  version?: string;
  isActive: boolean;
  updatedAt: string;
}


export interface UserProfile {
  userId?: string;
  email: string;
  phoneNumber?: string;
  identityNumber?: string;
  fullName?: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  placeOfOrigin?: string;
  placeOfResidence?: string;
  issueDate?: string;
  issuePlace?: string;
  verificationStatus?: string;
}

export interface UpdateProfilePayload {
  fullName: string;
  identityNumber: string;
  dateOfBirth: string;
  gender: string;
  nationality: string;
  placeOfOrigin: string;
  placeOfResidence: string;
  issueDate: string;
  issuePlace: string;
}

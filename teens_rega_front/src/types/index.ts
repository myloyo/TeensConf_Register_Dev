// [file name]: index.ts
export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  birthDate: string;
  phone: string;
  telegram: string;
  email: string;
  city: string;
  needAccommodation: boolean;
  church: string;
  role: 'подросток' | 'служитель';
  parentFullName?: string;
  parentPhone?: string;
  wasBefore: boolean;
  consentUnder14: boolean;
  consentDonation: boolean;
  consentPersonalData: boolean;
}

export interface RegistrationResponse {
  registrationId: number;
  message: string;
}

export interface PaymentCompletionRequest {
  paymentReference?: string;
  receiptFile?: File;
}

export interface PaymentCompletionResponse {
  success: boolean;
  receiptId?: number;
  verified: boolean;
  message: string;
  error?: string;
}

export interface ApiError {
  error: string;
}

export interface FormErrors {
  [key: string]: string;
}
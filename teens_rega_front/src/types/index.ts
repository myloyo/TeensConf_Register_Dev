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
  registrationId: BigInteger;
  qrCodeUrl: string;
  amount: BigInteger;
  message: string;
}

export interface PaymentConfirmationResponse {
  message: string;
}

export interface ApiError {
  error: string;
}

export interface FormErrors {
  [key: string]: string;
}
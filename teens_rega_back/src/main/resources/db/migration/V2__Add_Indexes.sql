CREATE INDEX idx_registrations_email ON registrations(email);
CREATE INDEX idx_registrations_phone ON registrations(phone);
CREATE INDEX idx_registrations_completed ON registrations(registration_completed_at);
CREATE INDEX idx_payment_receipts_reference ON payment_receipts(payment_reference);
CREATE INDEX idx_payment_receipts_verified ON payment_receipts(verified);

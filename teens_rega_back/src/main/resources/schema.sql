CREATE TABLE IF NOT EXISTS registrations (
                                             id BIGSERIAL PRIMARY KEY,
                                             first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    birth_date VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    telegram VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    need_accommodation BOOLEAN DEFAULT FALSE,
    church VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    parent_full_name VARCHAR(100),
    parent_phone VARCHAR(20),
    was_before BOOLEAN DEFAULT FALSE,
    donation_amount DOUBLE PRECISION DEFAULT 500.0,
    consent_under_14 BOOLEAN DEFAULT FALSE,
    consent_donation BOOLEAN DEFAULT FALSE,
    consent_personal_data BOOLEAN DEFAULT FALSE,
    paid BOOLEAN DEFAULT FALSE,
    sber_order_id VARCHAR(255),
    qr_payload TEXT,
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    payment_confirmed_at TIMESTAMP
    );

-- Создание таблицы payments
CREATE TABLE IF NOT EXISTS payments (
                                        id BIGSERIAL PRIMARY KEY,
                                        sber_order_id VARCHAR(255) UNIQUE,
    registration_id BIGINT,
    amount BIGINT,
    currency VARCHAR(10) DEFAULT 'RUB',
    status VARCHAR(50) DEFAULT 'PENDING',
    qr_payload TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    paid_at TIMESTAMP NULL,
    rrn VARCHAR(255),
    approval_code VARCHAR(255)
    );

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_registrations_phone ON registrations(phone);
CREATE INDEX IF NOT EXISTS idx_registrations_telegram ON registrations(telegram);
CREATE INDEX IF NOT EXISTS idx_registrations_paid ON registrations(paid);
CREATE INDEX IF NOT EXISTS idx_payments_sber_order_id ON payments(sber_order_id);
CREATE INDEX IF NOT EXISTS idx_payments_registration_id ON payments(registration_id);
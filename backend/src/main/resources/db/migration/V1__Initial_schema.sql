
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255)  NOT NULL UNIQUE,
    phone_number VARCHAR(20)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT chk_user_role CHECK (role IN ('CLIENT', 'NOTARY', 'ADMIN')),
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    CONSTRAINT chk_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

);
CREATE UNIQUE INDEX uk_users_email ON users(email);
CREATE UNIQUE INDEX uk_users_phone ON users(phone_number);

create table user_profiles (
    user_id UUID PRIMARY KEY references users(user_id) on delete cascade,

    identity_number varchar(20) ,
    full_name varchar(255),
    date_of_birth date,
    gender varchar(10),
    nationality varchar(50) default 'Vietnamese',
    place_of_origin text,
    place_of_residence text,
    issue_date date,
    issue_place text
);
CREATE UNIQUE INDEX uk_user_profiles_identity ON user_profiles(identity_number);


CREATE TABLE notary_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES users(user_id),
    notary_id UUID REFERENCES users(user_id),

    service_type VARCHAR(20) NOT NULL,
    CONSTRAINT chk_service_type CHECK (service_type IN ('ONLINE', 'OFFLINE')),

    contract_type VARCHAR(100) NOT NULL,
    description TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    CONSTRAINT chk_request_status CHECK (status IN ('NEW', 'PROCESSING', 'SCHEDULED', 'AWAITING_PAYMENT', 'COMPLETED', 'CANCELLED')),

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notary_requests_client ON notary_requests(client_id);
CREATE INDEX idx_notary_requests_notary ON notary_requests(notary_id);

CREATE TABLE appointments (
    appointment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES notary_requests(request_id) ON DELETE CASCADE,

    scheduled_time TIMESTAMPTZ NOT NULL,

    meeting_url VARCHAR(500),
    physical_address TEXT DEFAULT 'Văn phòng công chứng số 1',

    status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT chk_appointment_status CHECK (status IN ('PENDING', 'FINISHED', 'CANCELLED')),

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_appointments_request ON appointments(request_id);




CREATE TABLE documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES notary_requests(request_id) ON DELETE CASCADE,

    file_path VARCHAR(500) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    CONSTRAINT chk_doc_type CHECK (doc_type IN ('ID_CARD', 'PROPERTY_PAPER', 'DRAFT_CONTRACT', 'SIGNED_DOCUMENT', 'SESSION_VIDEO')),

    file_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_documents_request ON documents(request_id);


CREATE TABLE signatures (
    signature_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(document_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id),

    signature_value TEXT NOT NULL,
    cert_serial VARCHAR(100),
    signed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    is_valid BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_signatures_document ON signatures(document_id);

CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES notary_requests(request_id),

    amount DECIMAL(15, 2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED')),

    transaction_reference VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payments_request ON payments(request_id);

CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    action VARCHAR(255) NOT NULL,
    table_name VARCHAR(100),
    record_id UUID,
    old_value JSONB,
    new_value JSONB,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_modtime
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();

CREATE TRIGGER update_request_modtime
    BEFORE UPDATE ON notary_requests
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();

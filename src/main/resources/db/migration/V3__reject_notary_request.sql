ALTER TABLE notary_requests
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE notary_requests
    DROP CONSTRAINT IF EXISTS chk_request_status;

ALTER TABLE notary_requests
    ADD CONSTRAINT chk_request_status
        CHECK (status IN ('NEW', 'PROCESSING', 'SCHEDULED', 'AWAITING_PAYMENT', 'COMPLETED', 'CANCELLED', 'REJECTED'));


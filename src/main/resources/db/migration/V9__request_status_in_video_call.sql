-- Add IN_VIDEO_CALL to the request_status check constraint
-- This status indicates the notary is actively on a video call with the client for identity verification

ALTER TABLE notary_requests
    DROP CONSTRAINT IF EXISTS chk_request_status;

ALTER TABLE notary_requests
    ADD CONSTRAINT chk_request_status CHECK (
        status IN (
            'NEW',
            'PROCESSING',
            'ACCEPTED',
            'SCHEDULED',
            'IN_VIDEO_CALL',
            'AWAITING_PAYMENT',
            'COMPLETED',
            'CANCELLED',
            'REJECTED'
        )
    );

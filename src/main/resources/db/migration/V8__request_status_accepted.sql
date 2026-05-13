-- Split lifecycle meaning:
-- PROCESSING = request has enough required documents and is waiting for notary pickup.
-- ACCEPTED   = request has been accepted by a notary.

ALTER TABLE notary_requests
    DROP CONSTRAINT IF EXISTS chk_request_status;

ALTER TABLE notary_requests
    ADD CONSTRAINT chk_request_status CHECK (
        status IN (
            'NEW',
            'PROCESSING',
            'ACCEPTED',
            'SCHEDULED',
            'AWAITING_PAYMENT',
            'COMPLETED',
            'CANCELLED',
            'REJECTED'
        )
    );

UPDATE notary_requests
SET status = 'ACCEPTED'
WHERE status = 'PROCESSING'
  AND notary_id IS NOT NULL;


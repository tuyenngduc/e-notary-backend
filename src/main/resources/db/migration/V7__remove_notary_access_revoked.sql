-- Remove notary access revocation feature
ALTER TABLE users
DROP COLUMN IF EXISTS notary_access_revoked;


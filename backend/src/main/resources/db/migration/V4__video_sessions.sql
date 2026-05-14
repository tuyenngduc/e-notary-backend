-- Add video_sessions table for online video call meetings

CREATE TABLE video_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(appointment_id) ON DELETE CASCADE,

    session_token VARCHAR(255) UNIQUE NOT NULL,
    meeting_url VARCHAR(500),
    room_id VARCHAR(100) UNIQUE NOT NULL,

    status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT chk_video_session_status CHECK (status IN ('PENDING', 'NOTARY_JOINED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED')),

    notary_joined_at TIMESTAMPTZ,
    client_joined_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,

    duration_seconds BIGINT,

    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_video_sessions_appointment ON video_sessions(appointment_id);
CREATE INDEX idx_video_sessions_room_id ON video_sessions(room_id);
CREATE INDEX idx_video_sessions_status ON video_sessions(status);
CREATE INDEX idx_video_sessions_created_at ON video_sessions(created_at DESC);


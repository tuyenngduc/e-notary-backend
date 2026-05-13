export type VideoSessionStatus =
  | 'PENDING'
  | 'NOTARY_JOINED'
  | 'IN_PROGRESS'
  | 'FINISHED'
  | 'CANCELLED'
  | string;

export interface VideoSessionResponse {
  sessionId: string;
  appointmentId: string | null;
  sessionToken: string;
  meetingUrl: string;
  roomId: string;
  status: VideoSessionStatus;
  notaryJoinedAt: string | null;
  clientJoinedAt: string | null;
  endedAt: string | null;
  durationSeconds: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

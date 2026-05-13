export function getVideoRoomPathFromMeetingUrl(meetingUrl: string | null | undefined): string | null {
  if (!meetingUrl || !meetingUrl.trim()) {
    return null;
  }

  try {
    const url = new URL(meetingUrl, window.location.origin);
    const roomMatch = url.pathname.match(/\/(?:api\/video\/room|video\/room)\/([^/?#]+)/i);
    if (!roomMatch) {
      return null;
    }

    const roomId = roomMatch[1];
    const token = url.searchParams.get('token');
    const params = new URLSearchParams();

    if (token) {
      params.set('token', token);
    }

    const queryString = params.toString();
    return queryString ? `/video/room/${roomId}?${queryString}` : `/video/room/${roomId}`;
  } catch {
    return null;
  }
}

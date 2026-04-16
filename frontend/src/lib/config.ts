export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

const derivedWsBaseUrl = API_BASE_URL.replace(/^http:/, 'ws:').replace(/^https:/, 'wss:');
export const VIDEO_SIGNALING_WS_URL =
  import.meta.env.VITE_VIDEO_SIGNALING_WS_URL ?? `${derivedWsBaseUrl}/ws/video-signaling`;



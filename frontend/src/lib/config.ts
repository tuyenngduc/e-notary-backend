const envApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

// Use same-origin by default so Vite proxy can avoid CORS in local development.
export const API_BASE_URL = envApiBaseUrl && envApiBaseUrl.length > 0 ? envApiBaseUrl : '';

const wsBaseUrl =
  API_BASE_URL.length > 0
    ? API_BASE_URL.replace(/^http:/, 'ws:').replace(/^https:/, 'wss:')
    : window.location.origin.replace(/^http:/, 'ws:').replace(/^https:/, 'wss:');

export const VIDEO_SIGNALING_WS_URL =
  import.meta.env.VITE_VIDEO_SIGNALING_WS_URL ?? `${wsBaseUrl}/ws/video-signaling`;

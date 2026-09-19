const readEnv = (key: string, fallback: string): string => {
  const value = (import.meta as any).env?.[key]
  return value || fallback
}

// HBuilderX/Vite 会在编译时注入 VITE_ 环境变量。默认值仅面向本地开发。
export const BASE_URL = readEnv("VITE_API_BASE_URL", "http://127.0.0.1:8500")
export const BASE_UPLOAD_URL = readEnv("VITE_UPLOAD_BASE_URL", BASE_URL)
export const WebSocket_BASE_URL = readEnv(
  "VITE_WEBSOCKET_URL",
  "ws://127.0.0.1:8500/api/websocket",
)
export const MEDIA_BASE_URL = readEnv(
  "VITE_MEDIA_BASE_URL",
  "http://127.0.0.1:9000/luojia-soundscape/demo/audio",
)
export const LEGACY_MEDIA_HOST = readEnv("VITE_LEGACY_MEDIA_HOST", "")

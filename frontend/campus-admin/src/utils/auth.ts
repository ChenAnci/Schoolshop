/**
 * Token 本地存储
 * 骨架阶段先用 localStorage 保存 accessToken；refreshToken 预留字段，
 * 待后端双 Token（/auth/refresh）落地后启用。
 */
const TOKEN_KEY = 'Campus-Admin-Token'
const REFRESH_KEY = 'Campus-Admin-Refresh-Token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

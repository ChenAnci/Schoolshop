import { getToken } from '@/utils/auth'

type Handler = (data: any) => void
const handlers = new Map<string, Set<Handler>>()

let socket: WebSocket | null = null
let manualClose = false
let retry = 0
let pingTimer: ReturnType<typeof setInterval> | null = null
let pongMiss = 0

/** 计算 ws 地址（开发走 vite 代理 /dev-api/ws，生产可改为 wss 域名） */
function buildUrl(): string {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const base = import.meta.env.VITE_APP_BASE_API || ''
  return `${proto}://${location.host}${base}/ws?token=${encodeURIComponent(getToken() || '')}`
}

function startHeartbeat() {
  stopHeartbeat()
  pingTimer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'PING' }))
      pongMiss++
      if (pongMiss > 2) {
        socket.close()
      }
    }
  }, 30000)
}
function stopHeartbeat() {
  if (pingTimer) clearInterval(pingTimer)
  pingTimer = null
}

export function connect() {
  const token = getToken()
  if (!token || socket || manualClose) return
  socket = new WebSocket(buildUrl())
  socket.onopen = () => {
    retry = 0
    pongMiss = 0
    startHeartbeat()
  }
  socket.onmessage = (ev) => {
    let msg: any
    try { msg = JSON.parse(ev.data) } catch { return }
    if (msg.type === 'PONG') { pongMiss = 0; return }
    emit(msg.type, msg.data)
  }
  socket.onclose = () => {
    socket = null
    stopHeartbeat()
    if (!manualClose) {
      retry = Math.min(retry + 1, 6)
      const delay = Math.min(1000 * 2 ** (retry - 1), 30000)
      setTimeout(connect, delay)
    }
  }
  socket.onerror = () => { socket?.close() }
}

export function disconnect() {
  manualClose = true
  stopHeartbeat()
  socket?.close()
  socket = null
}

export function sendChat(receiverId: number, content: string, msgType = '1') {
  socket?.send(JSON.stringify({ type: 'CHAT_SEND', receiverId, content, msgType }))
}
export function sendRead(sessionId: number) {
  socket?.send(JSON.stringify({ type: 'CHAT_READ', sessionId }))
}

export function on(type: string, handler: Handler) {
  if (!handlers.has(type)) handlers.set(type, new Set())
  handlers.get(type)!.add(handler)
}
export function off(type: string, handler: Handler) {
  handlers.get(type)?.delete(handler)
}
function emit(type: string, data: any) {
  handlers.get(type)?.forEach((h) => h(data))
}

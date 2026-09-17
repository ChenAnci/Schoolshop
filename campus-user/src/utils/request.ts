import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken, getRefreshToken } from '@/utils/auth'
import router from '@/router'

/** 后端统一返回结构（与 RuoYi AjaxResult 对齐） */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data?: T
  token?: string
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000
})

// 是否正在刷新 token（占位：后端双 Token 落地后启用单飞去重）
let isRefreshing = false

/**
 * 401 处理：有 refreshToken 则尝试刷新并重放原请求；否则登出跳登录页。
 * 【占位】后端 /auth/refresh 尚未实现，此处先直接登出，接口已留好扩展点。
 */
async function refreshAccessToken(): Promise<string> {
  const refresh = getRefreshToken()
  if (!refresh) {
    throw new Error('no refresh token')
  }
  // TODO(M1): 调用 POST /auth/refresh { refreshToken }，返回新 accessToken
  // const { data } = await axios.post(`${import.meta.env.VITE_APP_BASE_API}/auth/refresh`, { refreshToken: refresh })
  // setToken(data.token)
  // return data.token
  throw new Error('refresh endpoint not implemented')
}

function toLogin() {
  removeToken()
  router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

// 请求拦截器：注入 token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一解包 + 错误处理 + 401 刷新
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    if (res.code === 401) {
      ElMessage.error(res.msg || '登录状态已过期')
      toLogin()
      return Promise.reject(res)
    }
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res as unknown as AxiosResponse
  },
  async (error) => {
    const status = error.response?.status
    const res = error.response?.data as ApiResult | undefined

    if (status === 401) {
      if (!isRefreshing) {
        isRefreshing = true
        try {
          await refreshAccessToken()
          // 刷新成功：重放原请求（占位，待 M1 启用）
        } catch {
          ElMessage.error(res?.msg || '登录状态已过期，请重新登录')
          toLogin()
        } finally {
          isRefreshing = false
        }
      }
    } else {
      ElMessage.error(res?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

// 泛型便捷方法
export function get<T = ApiResult>(url: string, config?: AxiosRequestConfig) {
  return service.get<T, T>(url, config)
}
export function post<T = ApiResult>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return service.post<T, T>(url, data, config)
}
export function put<T = ApiResult>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return service.put<T, T>(url, data, config)
}
export function del<T = ApiResult>(url: string, config?: AxiosRequestConfig) {
  return service.delete<T, T>(url, config)
}

export default service

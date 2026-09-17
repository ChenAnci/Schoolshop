import { post, get, del } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

export interface LoginData {
  username: string
  password: string
  code: string
  uuid: string
}

export interface CaptchaData {
  uuid: string
  img: string
  captchaEnabled: boolean
}

export interface UserInfo {
  user: Record<string, unknown>
  roles: string[]
  permissions: string[]
}

/** 登录（返回 { code, msg, token }） */
export function login(data: LoginData): Promise<ApiResult> {
  return post<ApiResult>('/login', data)
}

/** 登出 */
export function logout(): Promise<ApiResult> {
  return del<ApiResult>('/logout')
}

/** 图形验证码（响应拦截器已解包，字段在顶层） */
export function getCaptcha(): Promise<CaptchaData> {
  return get<CaptchaData>('/captchaImage')
}

/** 当前登录用户信息（角色/权限/用户；响应拦截器未额外解包，user/roles 在顶层） */
export function getInfo(): Promise<UserInfo> {
  return get<UserInfo>('/getInfo')
}

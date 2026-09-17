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

/** 路由 meta 信息 */
export interface RouterMeta {
  title?: string
  icon?: string
  noCache?: boolean
  link?: string
}

/** 后端动态路由节点（对应 RouterVo，component 为字符串路径） */
export interface RouterVo {
  name?: string
  path?: string
  hidden?: boolean
  redirect?: string
  component?: string
  alwaysShow?: boolean
  meta?: RouterMeta
  children?: RouterVo[]
}

/** 获取当前用户可访问的后端菜单路由（getRouters） */
export function getRouters(): Promise<ApiResult<RouterVo[]>> {
  return get<ApiResult<RouterVo[]>>('/getRouters')
}

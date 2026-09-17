import { get, put, del } from '@/utils/request'
import type { ApiResult } from '@/utils/request'
import type { TableData } from '@/api/market/order'

/** 用户信息（对应后端 SysUser，含商家入驻字段） */
export interface UserData {
  userId?: number
  userName?: string
  nickName?: string
  phonenumber?: string
  email?: string
  /** 账号状态 0正常 1停用 */
  status?: string
  /** 商家入驻申请状态 0未申请 1待审核 2已通过 3已驳回 */
  merchantApplyStatus?: string
  merchantApplyRemark?: string
  createTime?: string
  remark?: string
}

/** 商家入驻状态字典 */
export const MERCHANT_APPLY_STATUS: Record<string, { text: string; type: 'primary' | 'success' | 'danger' | 'warning' | 'info' }> = {
  '0': { text: '未申请', type: 'info' },
  '1': { text: '待审核', type: 'warning' },
  '2': { text: '已通过', type: 'success' },
  '3': { text: '已驳回', type: 'danger' }
}

/** 账号状态字典 */
export const USER_STATUS: Record<string, { text: string; type: 'success' | 'danger' }> = {
  '0': { text: '正常', type: 'success' },
  '1': { text: '停用', type: 'danger' }
}

/** 查询用户列表（账号/入驻状态筛选） */
export function listUser(params: Record<string, unknown>): Promise<TableData<UserData>> {
  return get<TableData<UserData>>('/system/user/list', { params })
}

/** 启用/停用账号 */
export function changeStatus(userId: number, status: string): Promise<ApiResult> {
  return put<ApiResult>('/system/user/changeStatus', { userId, status })
}

/** 重置密码 */
export function resetPwd(userId: number, password: string): Promise<ApiResult> {
  return put<ApiResult>('/system/user/resetPwd', { userId, password })
}

/** 删除账号 */
export function delUser(userId: number): Promise<ApiResult> {
  return del<ApiResult>(`/system/user/${userId}`)
}

/** 商家入驻审核：action approve(通过,自动授权商家角色)/reject(驳回) */
export function merchantAudit(userId: number, action: string, remark?: string): Promise<ApiResult> {
  return put<ApiResult>('/system/user/merchantAudit', { userId, action, remark })
}
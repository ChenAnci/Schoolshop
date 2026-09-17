import { get, post, put, del } from '@/utils/request'
import type { ApiResult } from '@/utils/request'
import type { TableData } from '@/api/market/order'

/** 角色信息（对应后端 SysRole） */
export interface RoleData {
  roleId?: number
  roleName?: string
  roleKey?: string
  roleSort?: number
  /** 状态 0正常 1停用 */
  status?: string
  remark?: string
  createTime?: string
  /** 编辑时回填的菜单权限点 */
  menuIds?: number[]
}

/** 角色状态字典 */
export const ROLE_STATUS: Record<string, { text: string; type: 'success' | 'danger' }> = {
  '0': { text: '正常', type: 'success' },
  '1': { text: '停用', type: 'danger' }
}

/** 查询角色列表 */
export function listRole(params: Record<string, unknown>): Promise<TableData<RoleData>> {
  return get<TableData<RoleData>>('/system/role/list', { params })
}

/** 角色详情（含已分配菜单权限点 menuIds） */
export function getRole(roleId: number): Promise<ApiResult<{ role: RoleData; menuIds: number[] }>> {
  return get<ApiResult<{ role: RoleData; menuIds: number[] }>>(`/system/role/${roleId}`)
}

/** 新增角色 */
export function addRole(data: RoleData): Promise<ApiResult> {
  return post<ApiResult>('/system/role', data)
}

/** 修改角色 */
export function updateRole(data: RoleData): Promise<ApiResult> {
  return put<ApiResult>('/system/role', data)
}

/** 启用/停用角色 */
export function changeRoleStatus(roleId: number, status: string): Promise<ApiResult> {
  return put<ApiResult>('/system/role/changeStatus', { roleId, status })
}

/** 删除角色 */
export function delRole(roleIds: number[]): Promise<ApiResult> {
  return del<ApiResult>(`/system/role/${roleIds.join(',')}`)
}
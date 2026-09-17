import { get, post, put, del } from '@/utils/request'
import type { ApiResult } from '@/utils/request'

/** 菜单/权限点（对应后端 SysMenu） */
export interface MenuData {
  menuId?: number
  parentId?: number
  parentName?: string
  menuName?: string
  orderNum?: number
  path?: string
  component?: string
  /** 菜单类型 M目录 C菜单 F按钮 */
  menuType?: 'M' | 'C' | 'F'
  visible?: string
  status?: string
  perms?: string
  icon?: string
  children?: MenuData[]
}

/** 菜单树节点（后端 /system/menu/treeselect 返回） */
export interface MenuTree {
  id: number
  label: string
  disabled?: boolean
  children?: MenuTree[]
}

/** 菜单状态字典 */
export const MENU_STATUS: Record<string, { text: string; type: 'success' | 'danger' }> = {
  '0': { text: '正常', type: 'success' },
  '1': { text: '停用', type: 'danger' }
}

/** 菜单类型字典 */
export const MENU_TYPE: Record<string, string> = {
  M: '目录',
  C: '菜单',
  F: '按钮'
}

/** 菜单可见字典 */
export const MENU_VISIBLE: Record<string, string> = {
  '0': '显示',
  '1': '隐藏'
}

/** 查询菜单列表（树形） */
export function listMenu(params: Record<string, unknown>): Promise<ApiResult<MenuData[]>> {
  return get<ApiResult<MenuData[]>>('/system/menu/list', { params })
}

/** 菜单详情 */
export function getMenu(menuId: number): Promise<ApiResult<MenuData>> {
  return get<ApiResult<MenuData>>(`/system/menu/${menuId}`)
}

/** 查询菜单树（用于上级菜单下拉/角色授权） */
export function menuTreeselect(): Promise<ApiResult<MenuTree[]>> {
  return get<ApiResult<MenuTree[]>>('/system/menu/treeselect')
}

/** 新增菜单 */
export function addMenu(data: MenuData): Promise<ApiResult> {
  return post<ApiResult>('/system/menu', data)
}

/** 修改菜单 */
export function updateMenu(data: MenuData): Promise<ApiResult> {
  return put<ApiResult>('/system/menu', data)
}

/** 删除菜单 */
export function delMenu(menuId: number): Promise<ApiResult> {
  return del<ApiResult>(`/system/menu/${menuId}`)
}
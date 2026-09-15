export interface ScopeInfo { id: string; code: string; name: string; defaultFlag: boolean; memberRole?: string }
export interface UserInfo {
  id: string; loginName: string; displayName: string; mode: 'single' | 'multi';
  scopes: ScopeInfo[]; currentScopeId?: string; defaultScope?: ScopeInfo;
  roles: string[]; permissions: string[]; platformPermissions: string[]; unitPermissions: string[];
}
export interface LoginResult { token: string; userInfo: UserInfo }
export interface Page<T> { records: T[]; total: number; current: number; size: number }
export interface Member {
  id: string; userId: string; scopeId: string; loginName: string; displayName: string;
  orgId?: string; status: number; version: number; createdAt: string;
}
export interface MemberDetail { member: Member; roleIds: string[] }
export interface Option { value: string; label: string }
export interface DepartmentOption extends Option { parentId?: string }
export interface Department {
  id: string; scopeId: string; parentId?: string; name: string; code?: string; version: number;
}
export interface CreateDepartment { name: string; code?: string; parentId?: string }
export interface EditDepartment { id: string; version: number; name: string }
export interface Role {
  id: string; scopeId: string; code: string; name: string; dataRange: number;
  status: number; sort: number; version: number; remark?: string;
}
export interface RoleMenuOption {
  id: string; parentId?: string; name: string; code: string; type: number;
}
export interface RoleDepartmentOption { id: string; parentId?: string; name: string }
export interface RoleOptions { menus: RoleMenuOption[]; departments: RoleDepartmentOption[] }
export interface RoleDetail { role: Role; menuIds: string[]; orgIds: string[]; editable: boolean }
export interface CreateRole {
  code: string; name: string; dataRange: number; status: number; sort: number;
  remark?: string; menuIds: string[]; orgIds: string[];
}
export interface EditRole extends Omit<CreateRole, 'code'> { id: string; version: number }
export interface CreateMember { loginName: string; password: string; displayName: string; orgId?: string }
export interface EditMember { version: number; displayName: string; orgId?: string; status: number }

export interface ScopeInfo { id: string; code: string; name: string; defaultFlag: boolean; memberRole?: string }
export interface UserInfo {
  id: string; loginName: string; displayName: string; mode: 'single' | 'multi';
  scopes: ScopeInfo[]; currentScopeId?: string; defaultScope?: ScopeInfo;
  roles: string[]; permissions: string[]; platformPermissions: string[]; unitPermissions: string[];
}
export interface LoginResult { token: string; userInfo: UserInfo }
export type ConfigurationType = 'SYSTEM' | 'THIRD_PARTY';
export interface Configuration {
  id: string; name: string; value: string | null; type: ConfigurationType;
  desp?: string; sort: number; version: number;
}
export interface CreateConfiguration {
  name: string; value?: string | null; type?: ConfigurationType; desp?: string; sort?: number;
}
export interface UpdateConfiguration {
  version: number; value: string | null; type: ConfigurationType; desp?: string; sort?: number;
}
export interface Dictionary {
  id: string; name: string; systemName: string; sort: number; version: number;
}
export interface CreateDictionary { id: string; name: string; systemName: string; sort?: number }
export interface UpdateDictionary { version: number; name: string; sort?: number }
export interface DictionaryItem {
  id: string; basicId: string; paramKey: string; paramValue: string;
  desp?: string; sort: number; version: number;
}
export interface CreateDictionaryItem { paramKey: string; paramValue: string; desp?: string; sort?: number }
export interface UpdateDictionaryItem { version: number; paramValue: string; desp?: string; sort?: number }
export interface Page<T> { records: T[]; total: number; current: number; size: number }
export interface Member {
  id: string; userId: string; scopeId: string; loginName: string; displayName: string;
  orgId?: string; status: number; version: number; createdAt: string;
}
export interface MemberDetail { member: Member; roleIds: string[] }
export interface Option { value: string; label: string }
export interface DepartmentOption extends Option { parentId?: string }
export interface DepartmentLeaderOption extends Option { disabled: boolean }
export interface Department {
  id: string; scopeId: string; parentId?: string; name: string; code?: string;
  leaderId?: string; leaderName?: string; leaderActive: boolean; version: number;
}
export interface CreateDepartment { name: string; code?: string; parentId?: string; leaderId?: string }
export interface EditDepartment { id: string; version: number; name: string; leaderId?: string }
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
export interface PlatformMenu {
  id: string; parentId: string; name: string; code: string; type: number;
  path?: string; component?: string; redirect?: string; icon?: string; sort: number;
  visible: number; status: number; platformOnly: number; keepAlive: number;
  alwaysShow: number; version: number;
}
export interface CreatePlatformMenu extends Omit<PlatformMenu, 'id' | 'version'> {}
export interface EditPlatformMenu extends Omit<PlatformMenu, 'code' | 'type'> {}
export interface PlatformRole {
  id: string; code: string; name: string; status: number; sort: number; version: number; remark?: string;
}
export interface PlatformRoleOptions { menus: RoleMenuOption[] }
export interface PlatformRoleDetail { role: PlatformRole; menuIds: string[]; editable: boolean }
export interface CreatePlatformRole {
  code: string; name: string; status: number; sort: number; remark?: string; menuIds: string[];
}
export interface EditPlatformRole extends Omit<CreatePlatformRole, 'code'> { id: string; version: number }
export interface PlatformAccount {
  id: string; loginName: string; realName?: string; status: number; roleIds: string[];
}
export interface AuditRecord {
  id: string; actorId: string; scopeId: string; resource: string; recordId: string;
  operation: string; authorizedView: string; outcome: string; occurredAt: string;
}

export interface LoginAuditRecord {
  id: string;
  loginName: string;
  userId?: string;
  scopeId?: string;
  clientIp?: string;
  outcome: string;
  reason?: string;
  occurredAt: string;
}
export interface CreateMember { loginName: string; password: string; displayName: string; orgId?: string }
export interface EditMember { version: number; displayName: string; orgId?: string; status: number }

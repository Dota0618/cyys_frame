export interface ScopeInfo { id: string; code: string; name: string; defaultFlag: boolean; parentId: string }
export interface NavigationMenu { id: string; parentId: string; name: string; path?: string; type: number; sort: number; icon?: string }
export interface UserInfo {
  id: string; loginName: string; displayName: string;
  scopes: ScopeInfo[]; currentScopeId?: string; defaultScope?: ScopeInfo;
  roles: string[]; permissions: string[]; homeScopeId?: string; orgId?: string; dataRange: number;
  navigation?: NavigationMenu[];
}
export interface LoginResult { token: string; userInfo: UserInfo }
export interface PublicConfiguration {
  SYSTEM_NAME?: string;
  SYSTEM_PROVINCE_CODE?: string;
  PWD_STRENGTH?: string;
  PWD_LOCK_NUM?: string;
  PWD_LOCK_HOUR?: string;
}
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
  systemBuiltin: boolean;
}
export interface CreateDictionary { id: string; name: string; systemName: string; sort?: number }
export interface UpdateDictionary { version: number; name: string; sort?: number }
export interface DictionaryItem {
  id: string; basicId: string; paramKey: string; paramValue: string;
  desp?: string; sort: number; version: number;
}
export interface DictionaryOption { value: string; label: string; disabled?: boolean }
export interface CreateDictionaryItem { paramKey: string; paramValue: string; desp?: string; sort?: number }
export interface UpdateDictionaryItem { version: number; paramValue: string; desp?: string; sort?: number }
export interface Page<T> { records: T[]; total: number; current: number; size: number }
export interface Member {
  id: string; userId: string; scopeId?: string; loginName: string; displayName: string;
  orgId?: string; status: number; version: number; dataRange: number; createdAt: string;
  scopeName?: string; orgName?: string;
}
export interface MemberDataPermissionInput { dataRange: number; orgIds: string[] }
export interface UpdateMemberDataPermission extends MemberDataPermissionInput { version: number }
export interface MemberDataPermission extends UpdateMemberDataPermission { memberId: string; departments: DepartmentOption[] }
export interface MemberDetail { member: Member; roleIds: string[] }
export interface Option { value: string; label: string }
export interface DepartmentOption extends Option { parentId?: string }
export interface DepartmentLeaderOption extends Option { disabled: boolean }
export interface Department {
  id: string; scopeId: string; parentId?: string; name: string; code?: string;
  leaderId?: string; leaderName?: string; leaderActive?: boolean; version: number;
}
export interface CreateDepartment { name: string; code?: string; parentId?: string; leaderId?: string }
export interface EditDepartment { id: string; version: number; name: string; leaderId?: string }
export interface Role {
  id: string; code: string; name: string;
  status: number; sort: number; version: number; remark?: string;
}
export interface RoleMenuOption {
  id: string; parentId?: string; name: string; code: string; type: number;
}
export interface RoleOptions { menus: RoleMenuOption[] }
export interface RoleDetail { role: Role; menuIds: string[]; editable: boolean }
export interface CreateRole {
  code: string; name: string; status: number; sort: number;
  remark?: string; menuIds: string[];
}
export interface EditRole extends Omit<CreateRole, 'code'> { id: string; version: number }
export interface PlatformMenu {
  id: string; parentId: string; name: string; code: string; type: number;
  path?: string; component?: string; redirect?: string; icon?: string; sort: number;
  visible: number; status: number; keepAlive: number;
  alwaysShow: number; version: number;
}
export interface CreatePlatformMenu extends Omit<PlatformMenu, 'id' | 'version'> {}
export interface EditPlatformMenu extends Omit<PlatformMenu, 'code' | 'type'> {}
export interface AuditRecord {
  id: string; userId: string; loginName?: string; realName?: string; scopeId: string;
  resource: string; recordId: string; operation: string; operationLabel?: string;
  operationDisabled?: boolean; authorizedView: string; outcome: string; outcomeLabel?: string;
  outcomeDisabled?: boolean; occurredAt: string;
}
export interface AuditOptions { operations: DictionaryOption[]; outcomes: DictionaryOption[] }

export interface LoginAuditRecord {
  id: string;
  loginName: string;
  realName?: string;
  userId?: string;
  scopeId?: string;
  clientIp?: string;
  outcome: string;
  outcomeLabel?: string;
  outcomeDisabled?: boolean;
  reason?: string;
  occurredAt: string;
}
export interface LoginAuditOptions { outcomes: DictionaryOption[] }
export interface CreateMember { loginName: string; password: string; displayName: string; orgId?: string; dataPermission?: MemberDataPermissionInput }
export interface EditMember { version: number; displayName: string; orgId?: string; status: number; dataPermission?: MemberDataPermissionInput }

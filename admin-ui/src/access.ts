import type { UserInfo } from './types/api';
import { session } from './utils/session';

export default (initialState?: { user?: UserInfo }) => {
  const user = initialState?.user;
  const selected = user?.mode === 'single' || !!session().scopeId;
  const all = !!session().allUnits;
  const platform = user?.platformPermissions || [];
  const unitHas = (permission: string) =>
    !!user?.unitPermissions.includes(permission);
  const platformHas = (permission: string) =>
    platform.includes('platform:scope:access') && platform.includes(permission);
  const canUseGlobal =
    platformHas('user:read') && platform.includes('platform:data:read-all');
  const canWrite = (permission: string) =>
    !all &&
    selected &&
    (unitHas(permission) ||
      (platformHas(permission) && platform.includes('platform:data:operate')));
  const canReadInSelectedUnit = (permission: string) =>
    !all &&
    selected &&
    (unitHas(permission) ||
      (platformHas(permission) &&
        (platform.includes('platform:data:read-all') ||
          platform.includes('platform:data:operate'))));
  return {
    canReadUsers: all
      ? canUseGlobal
      : selected &&
        (unitHas('user:read') ||
          canUseGlobal ||
          (platformHas('user:read') &&
            platform.includes('platform:data:operate'))),
    canCreateUsers: canWrite('user:create'),
    canEditUsers: canWrite('user:update'),
    canResetAccountPasswords: platformHas('platform:account:reset-password'),
    canGrantRoles: !all && selected && unitHas('role:grant'),
    canReadDepartments: canReadInSelectedUnit('department:read'),
    canCreateDepartments: canWrite('department:create'),
    canEditDepartments: canWrite('department:update'),
    canDeleteDepartments: canWrite('department:delete'),
    canReadRoles: !all && selected && unitHas('role:read'),
    canCreateRoles: !all && selected && unitHas('role:create') && unitHas('role:grant'),
    canEditRoles: !all && selected && unitHas('role:update') && unitHas('role:grant'),
    canDeleteRoles: !all && selected && unitHas('role:delete') && unitHas('role:grant'),
    canReadPlatformMenus: platformHas('menu:read'),
    canCreatePlatformMenus: platformHas('menu:create'),
    canEditPlatformMenus: platformHas('menu:update'),
    canDeletePlatformMenus: platformHas('menu:delete'),
    canReadPlatformRoles: platformHas('platform:role:read'),
    canCreatePlatformRoles: platformHas('platform:role:create'),
    canEditPlatformRoles: platformHas('platform:role:update'),
    canDeletePlatformRoles: platformHas('platform:role:delete'),
    canGrantPlatformRoles: platformHas('platform:role:grant'),
    canReadConfigurations: platformHas('configuration:read'),
    canCreateConfigurations: platformHas('configuration:create'),
    canEditConfigurations: platformHas('configuration:update'),
    canDeleteConfigurations: platformHas('configuration:delete'),
    canReadDictionaries: platformHas('dictionary:read'),
    canCreateDictionaries: platformHas('dictionary:create'),
    canEditDictionaries: platformHas('dictionary:update'),
    canDeleteDictionaries: platformHas('dictionary:delete'),
    canReadAudits: !all && selected && unitHas('audit:read'),
    canReadLoginAudits: !all && selected && unitHas('login-audit:read'),
    canManageCaches: !all && selected && unitHas('cache:user:refresh'),
    canReadFiles: canReadInSelectedUnit('file:read'),
    canDownloadFiles: canReadInSelectedUnit('file:content:read'),
    canUploadFiles: canWrite('file:upload'),
    canEditFiles: canWrite('file:update'),
    canDeleteFiles: canWrite('file:delete'),
    canMoveFiles: canWrite('file:move'),
    canCreateFileDirectories: canWrite('file:directory:create'),
    canEditFileDirectories: canWrite('file:directory:update'),
    canDeleteFileDirectories: canWrite('file:directory:delete'),
    canReadFileRecycle: canReadInSelectedUnit('file:recycle:read'),
    canRestoreFiles: canWrite('file:restore'),
    canPurgeFiles: canWrite('file:purge'),
    canReadOrgNotices: canReadInSelectedUnit('org-notice:read'),
    canReadSystemNotices: platformHas('system-notice:read'),
    canReadReleases: platformHas('release:read'),
    canCreateReleases: platformHas('release:create'),
    canEditReleases: platformHas('release:update'),
    canDeleteReleases: platformHas('release:delete'),
    canSelectReleaseFiles: !all && selected && platformHas('platform:data:operate'),
    canCreateSystemNotices: platformHas('system-notice:create'),
    canEditSystemNotices: platformHas('system-notice:update'),
    canDeleteSystemNotices: platformHas('system-notice:delete'),
    canCreateOrgNotices: canWrite('org-notice:create'),
    canEditOrgNotices: canWrite('org-notice:update'),
    canDeleteOrgNotices: canWrite('org-notice:delete'),
    canReadNoticeReceipts: canReadInSelectedUnit('org-notice:receipts:read'),
  };
};

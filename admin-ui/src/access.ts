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
    canGrantRoles: !all && selected && unitHas('role:grant'),
    canReadDepartments: canReadInSelectedUnit('department:read'),
    canCreateDepartments: canWrite('department:create'),
    canEditDepartments: canWrite('department:update'),
    canDeleteDepartments: canWrite('department:delete'),
    canReadRoles: !all && selected && unitHas('role:read'),
    canCreateRoles: !all && selected && unitHas('role:create') && unitHas('role:grant'),
    canEditRoles: !all && selected && unitHas('role:update') && unitHas('role:grant'),
    canDeleteRoles: !all && selected && unitHas('role:delete') && unitHas('role:grant'),
  };
};

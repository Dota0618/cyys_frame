package com.cyys.admin.menu.dto;

import com.cyys.admin.menu.model.SysMenu;

public record MenuView(String id, String parentId, String name, String code, int type,
                   String path, String component, String redirect, String icon,
                   int sort, int visible, int status,
                   int keepAlive, int alwaysShow, int version) {
    public static MenuView from(SysMenu menu) {
        return new MenuView(menu.getId(), menu.getParentId(), menu.getName(), menu.getCode(), menu.getType(),
                menu.getPath(), menu.getComponent(), menu.getRedirect(), menu.getIcon(), menu.getSort(),
                menu.getVisible(), menu.getStatus(), menu.getKeepAlive(),
                menu.getAlwaysShow(), menu.getVersion());
    }
}

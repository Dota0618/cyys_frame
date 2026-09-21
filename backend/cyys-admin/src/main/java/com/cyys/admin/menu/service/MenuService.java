package com.cyys.admin.menu.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.menu.dto.CreateMenuCommand;
import com.cyys.admin.menu.dto.UpdateMenuCommand;
import com.cyys.admin.menu.dto.MenuView;
import com.cyys.admin.menu.mapper.SysMenuMapper;
import com.cyys.admin.menu.model.SysMenu;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MenuService {
    private static final String ROOT = "0";

    private final SysMenuMapper mapper;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public List<MenuView> all() {
        return mapper.all().stream().map(MenuView::from).toList();
    }

    @Transactional(readOnly = true)
    public MenuView detail(String id) {
        return MenuView.from(requireMenu(mapper.detail(id)));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public MenuView create(CreateMenuCommand command) {
        validateParent(command.parentId(), null, command.type());
        var menu = new SysMenu();
        menu.setId(IdWorker.getIdStr());
        menu.setCode(command.code());
        menu.setType(command.type());
        apply(menu, command.parentId(), command.name(), command.path(), command.component(), command.redirect(),
                command.icon(), command.sort(), command.visible(), command.status(),
                command.keepAlive(), command.alwaysShow());
        try {
            requireOne(mapper.create(menu, actor()));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "菜单编码或父子关系冲突");
        }
        audit.platformSuccess("menu", menu.getId(), "CREATE");
        return MenuView.from(menu);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public MenuView edit(UpdateMenuCommand command) {
        security.lockAdministratorGuard();
        SysMenu menu = requireMenu(mapper.lock(command.id()));
        if (!Objects.equals(menu.getVersion(), command.version())) throw new ApiException(409, "菜单版本已变更");
        validateParent(command.parentId(), menu.getId(), menu.getType());
        apply(menu, command.parentId(), command.name(), command.path(), command.component(), command.redirect(),
                command.icon(), command.sort(), command.visible(), command.status(),
                command.keepAlive(), command.alwaysShow());
        requireOne(mapper.updateMenu(menu, actor()));
        security.requireUsableAdministrator();
        menu.setVersion(menu.getVersion() + 1);
        audit.platformSuccess("menu", menu.getId(), "UPDATE");
        return MenuView.from(menu);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(String id, int version) {
        security.lockAdministratorGuard();
        SysMenu menu = requireMenu(mapper.lock(id));
        if (!Objects.equals(menu.getVersion(), version)) throw new ApiException(409, "菜单版本已变更");
        if (mapper.hasChildren(id)) throw new ApiException(409, "菜单仍有子节点，不能删除");
        if (mapper.hasRoleReference(id)) throw new ApiException(409, "菜单仍被角色引用，不能删除");
        requireOne(mapper.softDelete(id, version, actor()));
        security.requireUsableAdministrator();
        audit.platformSuccess("menu", id, "DELETE");
    }

    private void validateParent(String parentId, String currentId, int type) {
        if (ROOT.equals(parentId)) {
            if(type==3) throw new ApiException(400,"按钮权限必须放在所属菜单下");
            return;
        }
        if (Objects.equals(parentId, currentId)) throw new ApiException(400, "菜单不能以自身为父节点");
        var parent=mapper.lockParent(parentId);
        if (parent == null) throw new ApiException(400, "父菜单不存在或不能挂在操作权限下");
        if(type==3 && parent.getType()!=2) throw new ApiException(400,"按钮权限必须放在所属菜单下");
        if (currentId != null && mapper.isDescendant(currentId, parentId)) {
            throw new ApiException(400, "父菜单不能选择当前菜单的后代");
        }
    }


    private static void apply(SysMenu menu, String parentId, String name, String path, String component,
                              String redirect, String icon, int sort, int visible, int status,
                              int keepAlive, int alwaysShow) {
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setRedirect(redirect);
        menu.setIcon(icon);
        menu.setSort(sort);
        menu.setVisible(visible);
        menu.setStatus(status);

        menu.setKeepAlive(keepAlive);
        menu.setAlwaysShow(alwaysShow);
    }

    private static SysMenu requireMenu(SysMenu menu) {
        if (menu == null) throw new ApiException(404, "菜单不存在");
        return menu;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "菜单记录已变更");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
}

package com.cyys.admin.dictionary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.dictionary.dto.*;
import com.cyys.admin.dictionary.mapper.DictionaryMapper;
import com.cyys.admin.dictionary.model.ParamBasic;
import com.cyys.admin.dictionary.model.ParamBasicType;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DictionaryService {
    private final DictionaryMapper mapper;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<DictionaryView> page(int page, int size, String name) {
        security.authorize("dictionary:read");
        return mapper.page(new Page<ParamBasic>(page, size), name).convert(DictionaryView::from);
    }

    @Transactional(readOnly = true)
    public DictionaryView detail(String id) {
        security.authorize("dictionary:read");
        return DictionaryView.from(requireDictionary(mapper.detail(id)));
    }

    @Transactional
    public DictionaryView create(CreateDictionaryCommand command) {
        security.authorize("dictionary:create");
        var row = new ParamBasic();
        // 旧工程的分类编号由用户输入，保留其字符串 ID 语义。
        row.setId(command.id());
        row.setName(command.name());
        row.setSystemName(command.systemName());
        row.setSort(command.sort() == null ? 0 : command.sort());
        try {
            requireOne(mapper.create(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "字典编号或内部名称已存在（包含已删除记录）");
        }
        audit.platformSuccess("dictionary", row.getId(), "CREATE");
        return DictionaryView.from(row);
    }

    @Transactional
    public DictionaryView update(String id, UpdateDictionaryCommand command) {
        security.authorize("dictionary:update");
        ParamBasic row = requireDictionary(mapper.lock(id));
        row.setVersion(command.version());
        row.setName(command.name());
        row.setSort(command.sort() == null ? 0 : command.sort());
        requireOne(mapper.update(row, actor()));
        row.setVersion(command.version() + 1);
        audit.platformSuccess("dictionary", id, "UPDATE");
        return DictionaryView.from(row);
    }

    @Transactional
    public void delete(String id, int version) {
        security.authorize("dictionary:delete");
        requireDictionary(mapper.lock(id));
        if (mapper.firstItem(id) != null) throw new ApiException(409, "字典仍有字典项，不能删除");
        requireOne(mapper.softDelete(id, version, actor()));
        audit.platformSuccess("dictionary", id, "DELETE");
    }

    @Transactional(readOnly = true)
    public IPage<DictionaryItemView> items(String basicId, int page, int size, String paramValue) {
        security.authorize("dictionary:read");
        requireDictionary(mapper.detail(basicId));
        return mapper.items(new Page<ParamBasicType>(page, size), basicId, paramValue)
                .convert(DictionaryItemView::from);
    }

    @Transactional(readOnly = true)
    public DictionaryItemView item(String basicId, String id) {
        security.authorize("dictionary:read");
        requireDictionary(mapper.detail(basicId));
        return DictionaryItemView.from(requireItem(mapper.item(basicId, id)));
    }

    @Transactional
    public DictionaryItemView createItem(String basicId, CreateDictionaryItemCommand command) {
        security.authorize("dictionary:create");
        requireDictionary(mapper.lock(basicId));
        var row = new ParamBasicType();
        row.setId(IdWorker.getIdStr());
        row.setBasicId(basicId);
        row.setParamKey(command.paramKey());
        row.setParamValue(command.paramValue());
        row.setDesp(command.desp());
        row.setSort(command.sort() == null ? 0 : command.sort());
        try {
            requireOne(mapper.createItem(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "当前字典内该编码已存在（包含已删除记录）");
        }
        audit.platformSuccess("dictionary-item", row.getId(), "CREATE");
        return DictionaryItemView.from(row);
    }

    @Transactional
    public DictionaryItemView updateItem(String basicId, String id, UpdateDictionaryItemCommand command) {
        security.authorize("dictionary:update");
        requireDictionary(mapper.lock(basicId));
        ParamBasicType row = requireItem(mapper.item(basicId, id));
        row.setVersion(command.version());
        row.setParamValue(command.paramValue());
        row.setDesp(command.desp());
        row.setSort(command.sort() == null ? 0 : command.sort());
        requireOne(mapper.updateItem(row, actor()));
        row.setVersion(command.version() + 1);
        audit.platformSuccess("dictionary-item", id, "UPDATE");
        return DictionaryItemView.from(row);
    }

    @Transactional
    public void deleteItem(String basicId, String id, int version) {
        security.authorize("dictionary:delete");
        requireDictionary(mapper.lock(basicId));
        requireOne(mapper.deleteItem(basicId, id, version, actor()));
        audit.platformSuccess("dictionary-item", id, "DELETE");
    }

    private static ParamBasic requireDictionary(ParamBasic row) {
        if (row == null) throw new ApiException(404, "字典不存在");
        return row;
    }

    private static ParamBasicType requireItem(ParamBasicType row) {
        if (row == null) throw new ApiException(404, "字典项不存在或不属于当前字典");
        return row;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "字典记录已变更或删除，请刷新后重试");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
}

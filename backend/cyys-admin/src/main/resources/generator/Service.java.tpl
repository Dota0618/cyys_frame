package @@PACKAGE@@.service;

import @@PACKAGE@@.dto.*;
import @@PACKAGE@@.mapper.@@CLASS@@Mapper;
import @@PACKAGE@@.model.@@CLASS@@;
@@TREE_IMPORT@@
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.department.mapper.SysOrgMapper;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.admin.dictionary.mapper.DictionaryOptionMapper;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class @@CLASS@@Service {
    private final @@CLASS@@Mapper mapper;
    private final SysOrgMapper departments;
    private final DictionaryOptionMapper dictionaries;
    private final DataAuditService audit;
    @@TREE_DEPENDENCY@@

    @Transactional(readOnly=true)
    public IPage<@@CLASS@@View> page(int page,int size@@SEARCH_SERVICE@@) {
        var query=new QueryWrapper<@@CLASS@@>().orderByDesc("created_at","id");
        @@SEARCH_QUERY@@
        var paging=new Page<@@CLASS@@>(page,size);paging.setOptimizeCountSql(false);
        return mapper.selectPage(paging,query).convert(@@CLASS@@View::from);
    }
    @Transactional(readOnly=true)
    public @@CLASS@@View detail(String id) { return @@CLASS@@View.from(require(id,false)); }

    @Transactional(readOnly=true)
    public List<@@CLASS@@View> choices() {
        var rows=mapper.choices();
        if(rows.size()>500) throw new ApiException(422,"树和选择列表最多 500 条，请按业务需求增加分级查询");
        return rows.stream().map(@@CLASS@@View::from).toList();
    }
    @Transactional(readOnly=true)
    public @@CLASS@@Options options(String id) {
        if(id!=null) require(id,false);
        Map<String,List<DictionaryOption>> result=new LinkedHashMap<>();
        @@DICT_OPTIONS@@
        return new @@CLASS@@Options(id==null?departments.parentOptionsForCreate():List.of(),@@TREE@@ && id==null?choices():List.of(),result);
    }
    @Transactional
    public @@CLASS@@View create(Create@@CLASS@@Command command) {
        if(departments.forReference(command.departmentId())==null) throw new ApiException(409,"所属部门不可选择");
        @@CLASS@@ previous=null;
        @@DICT_CHECK@@
        var row=new @@CLASS@@();row.setOrgId(command.departmentId());
        @@PARENT_ASSIGN@@
        @@ASSIGN@@
        requireOne(mapper.insert(row));
        if(mapper.selectById(row.getId())==null) throw new ApiException(403,"没有该部门的创建权限");
        audit.success("@@FEATURE@@",row.getId(),"CREATE");return @@CLASS@@View.from(row);
    }
    @Transactional
    public @@CLASS@@View update(String id,Update@@CLASS@@Command command) {
        var row=require(id,true);
        if(!row.getVersion().equals(command.version())) throw new ApiException(409,"记录已变化，请刷新");
        var previous=row;
        @@DICT_CHECK@@
        @@ASSIGN@@
        requireOne(mapper.updateById(row));audit.success("@@FEATURE@@",id,"UPDATE");return @@CLASS@@View.from(row);
    }
    @Transactional
    public void remove(String id,int version) {
        var row=require(id,true);
        if(row.getVersion()!=version) throw new ApiException(409,"记录已变化，请刷新");
        @@PARENT_DELETE@@
        requireOne(mapper.remove(id,version,ScopeContext.get().loginId()));audit.success("@@FEATURE@@",id,"DELETE");
    }
    private @@CLASS@@ require(String id,boolean lock) {
        var row=lock?mapper.lock(id):mapper.selectById(id);
        if(row==null) throw new ApiException(404,"记录不存在或不可访问");return row;
    }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"记录状态或权限已变化"); }
}

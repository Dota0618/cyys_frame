package com.cyys.admin.file.service;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.mapper.FileCleanupMapper;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.List;

/** 受控服务身份仅能过期上传与删除临时对象，不借用上传人的登录、权限或令牌。 */
@Service
public class FileCleanupService {
    private static final Logger LOG=LoggerFactory.getLogger(FileCleanupService.class);
    private static final String ACTOR="system:file-cleanup";
    private final FileCleanupMapper files;
    private final TemporaryFileCleaner cleaner;
    private final DataAuditService audit;
    private final DataIsolationProperties isolation;
    private final FileCleanupProperties settings;
    private final TransactionTemplate transactions;

    public FileCleanupService(FileCleanupMapper files,TemporaryFileCleaner cleaner,DataAuditService audit,
                              DataIsolationProperties isolation,FileCleanupProperties settings,PlatformTransactionManager manager) {
        this.files=files; this.cleaner=cleaner; this.audit=audit; this.isolation=isolation; this.settings=settings;
        transactions=new TransactionTemplate(manager);
    }

    @Transactional(propagation=Propagation.NEVER)
    public void runBatch() {
        if(ScopeContext.current().isPresent()) throw new IllegalStateException("文件维护任务不能复用请求身份");
        String ceiling=isolation.mode()==DataIsolationProperties.Mode.SINGLE ? isolation.singleScopeId() : null;
        for(var candidate:files.candidates(ceiling,LocalDateTime.now(),settings.getBatchSize())) {
            if(candidate.scopeId()==null || "GLOBAL".equals(candidate.scopeId())
                    || (ceiling!=null && !ceiling.equals(candidate.scopeId()))) throw new IllegalStateException("文件维护单位无效");
            ScopeContext.set(new RequestIdentity(ACTOR,candidate.scopeId(),List.of(),List.of(),List.of()));
            try {
                // 先提交过期状态，再进行任何物理删除；删除失败不能使任务重新变为可续传。
                transactions.executeWithoutResult(status -> {
                    bind();
                    var file=files.lockDue(candidate.id(),candidate.scopeId(),LocalDateTime.now());
                    if(file!=null && "UPLOADING".equals(file.getState())) {
                        requireOne(files.expire(file.getId(),file.getScopeId(),ACTOR,LocalDateTime.now()));
                        audit.success("file",file.getId(),"EXPIRE_UPLOAD");
                    }
                });
                transactions.executeWithoutResult(status -> {
                    bind();
                    var file=files.lockDue(candidate.id(),candidate.scopeId(),LocalDateTime.now());
                    if(file==null || "UPLOADING".equals(file.getState())) return;
                    cleaner.clean(file);
                    requireOne(files.cleaned(file.getId(),file.getScopeId(),LocalDateTime.now()));
                    audit.success("file",file.getId(),"CLEAN_UPLOAD");
                });
            } catch(RuntimeException failure) {
                try {
                    transactions.executeWithoutResult(status -> {
                        bind();
                        files.retry(candidate.id(),candidate.scopeId(),LocalDateTime.now().plusSeconds(settings.getRetrySeconds()));
                    });
                } catch(RuntimeException retryFailure) {
                    LOG.error("File cleanup retry state unavailable: scope={}, file={}",candidate.scopeId(),candidate.id());
                }
                // 不输出存储路径、凭据或底层异常文本，也不假报成功；下一轮继续其他文件。
                LOG.warn("File cleanup deferred: scope={}, file={}, failure={}",candidate.scopeId(),candidate.id(),failure.getClass().getSimpleName());
            } finally { ScopeContext.clear(); }
        }
    }

    private static void bind() { DataTransactionGuard.bind(ScopeContext.get(),DataPolicyRegistry.View.CURRENT_SCOPE); }
    private static void requireOne(int count) { if(count!=1) throw new IllegalStateException("文件维护状态已变化"); }
}

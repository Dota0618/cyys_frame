package @@PACKAGE@@.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** Only called after locking/authorizing the parent; fixed server scope, returns existence only. */
@Mapper
@ManualDataScope
public interface @@CLASS@@DependencyMapper {
    @@CHILD_MAPPER@@
}

package com.cyys.application;

import com.cyys.common.web.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import java.util.stream.IntStream;

/** 仅测试类路径，实际经过权限拦截器、响应回填和 HTTP 序列化。 */
@RestController
@RequestMapping("/api/test/cache-values")
public class CacheValueProbe {
    @GetMapping @SaCheckPermission("test:read")
    public Object page() {
        var page=new Page<CacheValueProbeView>(1,50,50);
        page.setRecords(IntStream.range(0,50).mapToObj(i->new CacheValueProbeView("u-bob",null,null,"1",null,null,null)).toList());
        return R.ok(page);
    }
}

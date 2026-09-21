package com.cyys.application;

import com.cyys.admin.serial.service.SerialNumberService;
import com.cyys.common.mybatis.DataPermissionContext;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 仅测试父进程调用；两个 JVM 使用真实 Service/事务/MySQL，未增加取号 HTTP 接口。 */
public final class SerialNumberProcessWorker {
    public static void main(String[] args) throws Exception {
        if(args.length!=2 || !args[1].matches("[ab]")) throw new IllegalArgumentException("Expected fixture directory and worker a/b");
        Path directory=Path.of(args[0]).toAbsolutePath().normalize();
        if(!directory.startsWith(Path.of("target").toAbsolutePath().normalize()) || !Files.isRegularFile(directory.resolve("fixture-owned")))
            throw new IllegalArgumentException("Parent fixture marker missing");
        var options=new ArrayList<>(List.of("--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=ERROR",
                "--logging.level.com.cyys=ERROR", "--spring.main.banner-mode=off", "--cyys.file-cleanup.enabled=false",
                "--cyys.data-isolation.mode=multi", "--cyys.data-isolation.single-scope-id="));
        for(String code:List.of("ticket","order")) {
            String prefix="--cyys.serial-numbers.definitions."+code+".";
            options.add(prefix+"prefix="+(code.equals("ticket")?"TKT-":"ORD-"));
            options.add(prefix+"period="+(code.equals("ticket")?"NONE":"DAY"));
            options.add(prefix+"width="+(code.equals("ticket")?"2":"3"));
            options.add(prefix+"zone=Asia/Shanghai"); options.add(prefix+"permission=user:create");
        }
        try(var context=new SpringApplicationBuilder(CyysApplication.class).run(options.toArray(String[]::new))) {
            var service=context.getBean(SerialNumberService.class);
            Files.writeString(directory.resolve(args[1]+".ready"),Long.toString(ProcessHandle.current().pid()));
            Instant deadline=Instant.now().plusSeconds(45);
            while(!Files.exists(directory.resolve("go"))) {
                if(Instant.now().isAfter(deadline)) throw new IllegalStateException("Parent barrier timed out");
                Thread.sleep(25);
            }
            var results=new ArrayList<String>();
            for(int i=0;i<24;i++) results.add("A:"+next(service,"u-alice","scope-a","ticket"));
            results.add("B:"+next(service,"u-bob","scope-b","ticket"));
            results.add("ORDER:"+next(service,"u-alice","scope-a","order"));
            Files.write(directory.resolve(args[1]+".results"),results);
        }
    }
    private static String next(SerialNumberService service,String user,String scope,String code) {
        ScopeContext.set(new RequestIdentity(user,scope,List.of(),List.of()));
        DataPermissionContext.set("user:create",DataPolicyRegistry.View.CURRENT_SCOPE);
        try {return service.next(code);}
        finally {DataPermissionContext.clear(); ScopeContext.clear();}
    }
}

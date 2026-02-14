package br.com.sawcunhaos.foundation.audit.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
public final class ThreadsScosAuditConfiguration {

    @Bean(name = "ScosAuditLogAsyncExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(30);
        scheduler.setThreadNamePrefix("vt-audit-sch-");
        scheduler.setVirtualThreads(true);
        return scheduler;
    }
}

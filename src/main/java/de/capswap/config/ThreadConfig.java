package de.capswap.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.apache.coyote.http11.AbstractHttp11Protocol;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class ThreadConfig {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // nötig für async
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores * 2);
        executor.setQueueCapacity(cores * 20);
        executor.setThreadNamePrefix("capswap-async-");
        executor.initialize();
        log.info("Async-ThreadPool initialisiert (NFA 3): {} Kerne erkannt, corePoolSize={}, maxPoolSize={}.",
                cores, executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(2, cores / 2);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("capswap-scheduler-");
        scheduler.initialize();
        log.info("Scheduler-ThreadPool initialisiert (NFA 3): {} Kerne erkannt, poolSize={}.", cores, poolSize);
        return scheduler;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            int cores = Runtime.getRuntime().availableProcessors();
            int maxThreads = Math.max(200, cores * 25);
            int minSpareThreads = Math.max(10, cores * 2);

            var handler = connector.getProtocolHandler();
            if (handler instanceof AbstractHttp11Protocol<?> protocol) {
                protocol.setMaxThreads(maxThreads);
                protocol.setMinSpareThreads(minSpareThreads);
                log.info("Tomcat-ThreadPool initialisiert (NFA 3, REST-Verarbeitung): {} Kerne erkannt, maxThreads={}, minSpareThreads={}.",
                        cores, maxThreads, minSpareThreads);
            }
        });
    }
}

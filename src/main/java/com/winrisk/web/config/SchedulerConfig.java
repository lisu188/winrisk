package com.winrisk.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class SchedulerConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService aiScheduler() {
        return Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "ai-stepper");
            thread.setDaemon(true);
            return thread;
        });
    }
}

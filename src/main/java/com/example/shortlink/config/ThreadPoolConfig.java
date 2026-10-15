package com.example.shortlink.config;


import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {



    @Bean(name = "ShortLinkExecutor",destroyMethod = "shutdown")
    public ScheduledExecutorService accessLogExecutor() {
        return new ScheduledThreadPoolExecutor(
                2,
                new ThreadFactory() {
                    private final AtomicInteger index = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        Thread t = new Thread(r, "my-pool-" + index.getAndIncrement());
                        t.setUncaughtExceptionHandler((thread, ex) ->
                                System.err.println("线程 " + thread.getName() + " 异常: " + ex));
                        return t;
                    }
                },
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}

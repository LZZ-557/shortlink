package com.example.shortlink.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {



    @Bean(name = "AccessLogExecutor",destroyMethod = "shutdown")
    public ExecutorService accessLogExecutor() {
        return new ThreadPoolExecutor(
                2,
                4,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadFactory() {           // 自定义线程工厂
                    private final AtomicInteger index = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "my-pool-" + index.getAndIncrement());
                        t.setUncaughtExceptionHandler((thread, ex) -> {
                            System.err.println("线程 " + thread.getName() + " 异常: " + ex);
                        });
                        return t;
                    }
                },
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}

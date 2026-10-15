package com.example.shortlink;


import com.example.shortlink.service.component.IdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.concurrent.*;

@SpringBootTest
public class IdGeneratorConcurrencyTest {

    @Autowired
    private IdGenerator idGenerator;

    private static final int THREADS = 10;// 线程数
    private static final int PER_THREAD = 100_000;

    @Test
    void concurrentNextIdShouldNotDuplicate() throws Exception {
        Set<Long> ids = ConcurrentHashMap.newKeySet();   // 线程安全容器
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(THREADS);

        for(int i=0;i<THREADS;i++){
            pool.submit(() ->{
                try{
                    start.await();
                    for(int j=0;j<PER_THREAD;j++){
                        ids.add(idGenerator.nextId());
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally {
                    end.countDown();
                }
            });
        }

        start.countDown();
        end.await();

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long expection = THREADS * PER_THREAD;
        System.out.println("期望数："+expection);
        System.out.println("实际数："+ids.size());
        System.out.println("id重复数："+(expection - ids.size()));
        Assertions.assertEquals((long) THREADS * PER_THREAD, ids.size(), "出现重复");

    }


}

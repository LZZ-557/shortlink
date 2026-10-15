package com.example.shortlink.service.component;


import com.example.shortlink.config.ThreadPoolConfig;
import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.pojo.AccessLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Component
public class AccessLogRecorder {

    @Autowired
    ShortLinkMapper shortLinkMapper;

    @Autowired
    @Qualifier("AccessLogExecutor")
    private ExecutorService executor;


    public void record(AccessLog accessLog) {
//        异步写库，启动线程池
        executor.execute(() -> {
            try {
                shortLinkMapper.addAccessLog(accessLog);
            } catch (Exception e) {
                log.warn("访问日志写入失败: {}", e.getMessage());
            }
        });
//       shortLinkMapper.addAccessLog(accessLog);//这是测试同步写库的代码
    }
}

package com.example.shortlink.service.component;


import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.pojo.AccessLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class AsynchronousComponent {

    @Autowired
    ShortLinkMapper shortLinkMapper;

    @Autowired
    @Qualifier("ShortLinkExecutor")
    private ScheduledExecutorService executor;

    @Autowired
    private RedisService redisService;

    private static final long DELETE_DELAY_MS = 200;    // 延迟量

    public void AccessLogRecord(AccessLog accessLog) {
//        异步写库，启动线程池
        executor.execute(() -> {
            try {
                shortLinkMapper.addAccessLog(accessLog);
                log.info("访问日志写入成功：{}", accessLog.getShortCode());
            } catch (Exception e) {
                log.warn("访问日志写入失败: {}", e.getMessage());
            }
        });
//       shortLinkMapper.addAccessLog(accessLog);//这是测试同步写库的代码
    }

    //延时双删Redis
    public void deleteRedisTwice(String shortCode){
        redisService.delete("shortLink:"+shortCode);
        log.info("第一次删除缓存映射：{}",shortCode);

        executor.schedule(() ->{
            try{
                redisService.delete("shortLink:"+shortCode);
                log.info("第二次删除缓存映射：{}", shortCode);
            } catch (Exception e) {
                log.warn("第二次删除失败：{}", e.getMessage());
            }
        },DELETE_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}

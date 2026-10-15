package com.example.shortlink.controller;


import com.example.shortlink.pojo.AccessLog;
import com.example.shortlink.pojo.Result;
import com.example.shortlink.pojo.ShortLinkInformation;
import com.example.shortlink.pojo.StatsData;
import com.example.shortlink.service.ShortLinkService;
import com.example.shortlink.service.component.AccessLogRecorder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;

@Slf4j
@RestController
public class ShortLinkController {
    @Autowired
    private ShortLinkService shortLinkService;

    @Autowired
    private AccessLogRecorder recorder;

    //通过短码查询信息
    @GetMapping("/api/shortlink/{shortCode}")
    public Result selectActivityByShortCode(@PathVariable String shortCode){
        ShortLinkInformation sli = shortLinkService.selectActiveByShortCode(shortCode);
        if(sli == null) return Result.error("短码已失效");
        log.info("根据短码：{} 查找到了映射信息",shortCode);
        return Result.success(sli);
    }


    //通过短码查询信息，先查Reidis再查数据库
    @GetMapping("/api/shortlink/{shortCode}/cached")
    public Result queryWithCache(@PathVariable String shortCode){
        String longUrl = shortLinkService.RedisSelectByShortCode(shortCode);
        if(longUrl == null) return Result.error("短码已失效");
        log.info("根据短码：{} 查找到了映射信息",shortCode);
        return Result.success(longUrl);
    }


    //获取短码，并生成映射，然后将映射存入数据库中,最后将短码返回到前端
    @PostMapping("/api/shortlink")
    public Result addShortLink(@RequestBody ShortLinkInformation sli){
        String shortLink = shortLinkService.addShortLink(sli.getLongUrl());
        return Result.success(shortLink);
    }


    //写时删缓存
    @DeleteMapping("/api/shortlink/{shortCode}/delete")
    public Result softDeleteByShortCode(@PathVariable String shortCode){
        shortLinkService.softDeleteByShortCode(shortCode);

        return Result.success("已删除："+shortCode);
    }


    //重新启用映射
    @PostMapping("/api/shortlink/{shortCode}/enable")
    public Result enableMappingByShortCode(@PathVariable String shortCode){
        shortLinkService.enableMappingByShortCode(shortCode);

        return Result.success("重新启用："+shortCode);
    }


    //统计短链访问记录
    @GetMapping("/api/shortlink/{shortCode}/stats")
    public Result statisticAccessLogByShortCode(@PathVariable String shortCode){
        StatsData sd = shortLinkService.statisticAccessLogByShortCode(shortCode);
        if(sd == null){
            return Result.error("短码无效");
        }
        return Result.success(sd);
    }


    //点击短码跳转长链
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request){
        //访问时间先确定，而不是等到写库时才设置
        LocalDateTime accessTime = LocalDateTime.now();
        //调用接口通过短码查找缓存和数据库映射得到长链，没找到则返回：
        // 状态码：404
        //响应体：null
        //响应头：默认空
        String longUrl = shortLinkService.RedisSelectByShortCode(shortCode);
        if(longUrl == null){
            return ResponseEntity.notFound().build();
        }
        //通过request获取访问ip，客户端user_agent，跳转页面referer
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        //这里做个判断空，因为通过request不一定能获取到所以信息
        if (ua != null && ua.length() > 512) ua = ua.substring(0, 512);
        if (referer != null && referer.length() > 2048) referer = referer.substring(0, 2048);
        //判断完后才封装进accessLog
        AccessLog accessLog = new AccessLog();
        accessLog.setAccessTime(accessTime);
        accessLog.setVisitorIp(ip);
        accessLog.setShortCode(shortCode);
        accessLog.setUserAgent(ua);
        accessLog.setReferer(referer);
        //调用record方法，这是一个异步写入，如果用户访问时同时写入会有很长的跳转时间，
        // 所以是将写库的任务放入线程池中通过线程接收任务异步写入，放完任务后直接返回减小响应时间
        recorder.record(accessLog);

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl)).build();
    }











}

package com.example.shortlink.service.impl;


import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.pojo.ShortLinkInformation;
import com.example.shortlink.pojo.StatsData;
import com.example.shortlink.service.ShortLinkService;
import com.example.shortlink.service.component.AsynchronousComponent;
import com.example.shortlink.service.component.IdGenerator;
import com.example.shortlink.service.component.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
public class ShortLinkServiceImpl implements ShortLinkService {


    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    AsynchronousComponent asynchronousComponent;

    @Override
    public ShortLinkInformation selectActiveByShortCode(String shortCode) {
        return shortLinkMapper.selectActiveByShortCode(shortCode);
    }

    @Override
    public String addShortLink(String longUrl) {
        //先通过url_hash查重
        String urlHash = DigestUtils.md5DigestAsHex(longUrl.getBytes(StandardCharsets.UTF_8));
        ShortLinkInformation selectByHash = shortLinkMapper.selectActiveByHash(urlHash);
        if(selectByHash != null) return selectByHash.getShortCode();

        //加一层并发保护，如果两个用户同时发送请求，第一个用户存入映射成功，那么第二个用户存入映射时会被唯一索引挡住，返回500，所以要在这里加一个catch-try

        try{
            //走到这一步说明库中没有当前长链的映射，创建映射并存入库中
            Long id = idGenerator.nextId();
            String shortCode = idGenerator.idToShortCode(id);
            shortLinkMapper.addMapping(id, shortCode, longUrl, urlHash);
            return shortCode;
        }catch (DuplicateKeyException e){
            selectByHash = shortLinkMapper.selectActiveByHash(urlHash);
            if(selectByHash != null){
                return selectByHash.getShortCode();
            }
            throw new RuntimeException("短码冲突（id 重号），请重试", e);
        }

    }

    @Override
    public String RedisSelectByShortCode(String code) {
        String cached = redisService.get("shortLink:"+code);
        if(cached != null){
            return cached;
        }
        ShortLinkInformation sli = selectActiveByShortCode(code);
        if(sli == null){
            redisService.set("shortLink:"+code, null, Duration.ofDays(7));
            return null;
        }
        redisService.set("shortLink:"+code, sli.getLongUrl(), Duration.ofDays(7));
        return sli.getLongUrl();
    }

    @Override
    public void softDeleteByShortCode(String shortCode) {
        ShortLinkInformation sli = shortLinkMapper.selectByShortCode(shortCode);
        if(sli == null){
            throw new RuntimeException("短码所在记录不存在");
        }
        if(sli.getStatus() != null && sli.getStatus() == 1){
            shortLinkMapper.softDeleteByShortCode(shortCode);
            log.info("数据库软删除映射：{}",shortCode);

            asynchronousComponent.deleteRedisTwice(shortCode);

        }
        else log.info("映射已被删除");
    }

    @Override
    public void enableMappingByShortCode(String shortCode) {
        //查记录
        ShortLinkInformation sli = shortLinkMapper.selectByShortCode(shortCode);
        if(sli == null){
            throw new RuntimeException("短码所在记录不存在");
        }
        //判断status
        if(sli.getStatus() != null && sli.getStatus() == 0){
            shortLinkMapper.enableByShortCode(shortCode);
            log.info("重新启用映射：{}",shortCode);
        }
        else log.info("映射本身没有失效：{}",shortCode);
    }

    @Override
    public StatsData statisticAccessLogByShortCode(String shortCode) {
        if(shortLinkMapper.selectActiveByShortCode(shortCode) == null ){
            return null;
        }
        StatsData sd = shortLinkMapper.statisticAccessLogByShortCode(shortCode);
        sd.setShortCode(shortCode);
        return sd;
    }

}

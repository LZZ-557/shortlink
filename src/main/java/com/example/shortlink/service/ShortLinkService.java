package com.example.shortlink.service;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.pojo.ShortLinkInformation;
import com.example.shortlink.pojo.StatsData;

public interface ShortLinkService {
     ShortLinkInformation selectActiveByShortCode(String shortCode) ;

     String addShortLink(String longUrl);

     String RedisSelectByShortCode(String code);

     void softDeleteByShortCode(String shortCode);

     void enableMappingByShortCode(String shortCode);

     StatsData statisticAccessLogByShortCode(String shortCode);
}

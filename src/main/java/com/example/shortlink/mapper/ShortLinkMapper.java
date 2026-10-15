package com.example.shortlink.mapper;

import com.example.shortlink.pojo.AccessLog;
import com.example.shortlink.pojo.ShortLinkInformation;
import com.example.shortlink.pojo.StatsData;
import org.apache.ibatis.annotations.*;


@Mapper
public interface ShortLinkMapper {
    @Select("select id, short_code, long_url, url_hash, create_time, expire_time from t_short_link where short_code = #{shortCode} and status = 1")
    ShortLinkInformation selectActiveByShortCode(String shortCode);

    @Select("select id, short_code, long_url, url_hash, create_time, expire_time from t_short_link where url_hash = #{urlHash} and status = 1")
    ShortLinkInformation selectActiveByHash(String urlHash);

    @Select("select id, short_code, long_url, url_hash, status, create_time, expire_time from t_short_link where short_code = #{shortCode}")
    ShortLinkInformation selectByShortCode(String shortCode);

    @Insert("INSERT INTO t_short_link (id, short_code, long_url, url_hash) VALUES (#{id}, #{shortCode}, #{longUrl}, #{urlHash})")
    void addMapping(@Param("id") Long id, @Param("shortCode")String shortCode, @Param("longUrl") String longUrl, @Param("urlHash") String urlHash);

    @Insert("INSERT INTO t_access_log (short_code, access_time, visitor_ip, user_agent, referer) " +
            "VALUES (#{shortCode}, #{accessTime}, #{visitorIp}, #{userAgent}, #{referer})")
    void addAccessLog(AccessLog log);

    @Update("update t_short_link set status = 0 where short_code = #{shortCode}")
    void softDeleteByShortCode(String shortCode);

    @Update("update t_short_link set status = 1 where short_code = #{shortCode}")
    void enableByShortCode(String shortCode);

    @Select("select count(t_access_log.short_code) as visitCount, " +
            "max(t_access_log.access_time) as lastVisitTime " +
            "from t_access_log where t_access_log.short_code = #{shortCode};")
    StatsData statisticAccessLogByShortCode(String shortCode);
}

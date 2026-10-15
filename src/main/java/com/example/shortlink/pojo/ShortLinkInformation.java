package com.example.shortlink.pojo;

import lombok.Data;
import java.time.LocalDateTime;




@Data
public class ShortLinkInformation {
    private Long id;
    private String shortCode;
    private String longUrl;
    private String urlHash;
    private Short status;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}

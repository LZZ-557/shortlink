package com.example.shortlink.pojo;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccessLog {

    private Long accessId;
    private String shortCode;
    private LocalDateTime accessTime;
    private String visitorIp;
    private String userAgent;
    private String referer;

}

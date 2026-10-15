package com.example.shortlink.pojo;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StatsData {
    private String shortCode;
    private Integer visitCount;
    private LocalDateTime lastVisitTime;
}

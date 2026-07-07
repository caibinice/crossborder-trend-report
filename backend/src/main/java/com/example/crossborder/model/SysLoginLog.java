package com.example.crossborder.model;
import java.time.Instant;
public record SysLoginLog(long id,String tenantId,String username,String ipaddr,String status,String message,Instant createdAt) {}

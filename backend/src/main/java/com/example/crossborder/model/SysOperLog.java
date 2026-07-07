package com.example.crossborder.model;
import java.time.Instant;
public record SysOperLog(long id,String tenantId,String username,String module,String action,String method,String status,String message,Instant createdAt) {}

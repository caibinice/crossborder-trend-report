package com.example.crossborder.model;
public record AdminUser(long id,String tenantId,String username,String password,String nickname,String roleKey,String status,String email,String phone) {}

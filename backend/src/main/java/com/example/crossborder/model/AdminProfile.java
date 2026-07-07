package com.example.crossborder.model;
import java.util.List;
public record AdminProfile(String username,String nickname,String tenantId,List<String> roles,List<String> permissions) {}

package com.example.crossborder.model;
import java.util.List;
public record AdminRole(long id,String tenantId,String roleKey,String roleName,String status,List<String> menuKeys,String remark) {}

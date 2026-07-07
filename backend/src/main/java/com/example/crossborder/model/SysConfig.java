package com.example.crossborder.model;
public record SysConfig(long id,String tenantId,String configName,String configKey,String configValue,boolean systemBuiltin,String remark) {}

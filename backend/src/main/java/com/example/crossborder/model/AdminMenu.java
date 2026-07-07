package com.example.crossborder.model;
import java.util.List;
public record AdminMenu(long id,long parentId,String menuKey,String title,String icon,String path,String component,int sortOrder,String status,List<AdminMenu> children) {}

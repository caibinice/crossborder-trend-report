package com.example.crossborder.model;
import java.util.List;
public record AdminProfile(String username,String nickname,List<String> roles,List<String> permissions) {}

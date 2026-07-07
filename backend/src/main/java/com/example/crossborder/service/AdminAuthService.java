package com.example.crossborder.service;
import org.springframework.stereotype.Service;
@Service
public class AdminAuthService {
    public static final String ADMIN_TOKEN = "admin-token";
    public boolean login(String username, String password) { return "admin".equals(username) && "admin".equals(password); }
    public boolean authorized(String authorization) { return ("Bearer " + ADMIN_TOKEN).equals(authorization); }
}

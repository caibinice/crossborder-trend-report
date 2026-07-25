package com.example.crossborder.controller;

import com.example.crossborder.repository.AdminDataRepository;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.ApiValidationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/action-auth")
public class ActionAuthController {
    private final AdminDataRepository adminData;
    private final AdminAuthService auth;

    public ActionAuthController(AdminDataRepository adminData, AdminAuthService auth) {
        this.adminData = adminData;
        this.auth = auth;
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody PasswordRequest request) {
        if (request == null || request.password() == null || request.password().isBlank()) {
            throw new ApiValidationException("操作密码不能为空");
        }
        if (request.password().length() > 72) {
            throw new ApiValidationException("操作密码长度不能超过 72 个字符");
        }
        adminData.ensureSeedData();
        if (!adminData.validateLogin("admin", request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "操作密码错误");
        }
        return Map.of(
            "token", auth.issueActionToken(),
            "tokenType", "Bearer",
            "expiresIn", 30 * 60
        );
    }

    public record PasswordRequest(String password) {}
}

package com.example.crossborder.controller;
import com.example.crossborder.model.AdminLoginRequest;
import com.example.crossborder.model.AdminLoginResponse;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.AdminSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminSettingsService settings;
    public AdminController(AdminAuthService auth, AdminSettingsService settings) { this.auth = auth; this.settings = settings; }

    @PostMapping("/login")
    public AdminLoginResponse login(@RequestBody AdminLoginRequest request) {
        if (!auth.login(request.username(), request.password())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        return new AdminLoginResponse(AdminAuthService.ADMIN_TOKEN, "admin");
    }

    @GetMapping("/settings")
    public AdminSettings get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return settings.get();
    }

    @PutMapping("/settings")
    public AdminSettings save(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminSettings request) {
        requireAuth(authorization);
        return settings.save(request);
    }

    private void requireAuth(String authorization) {
        if (!auth.authorized(authorization)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "admin login required");
    }
}

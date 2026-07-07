
package com.example.crossborder.controller;

import com.example.crossborder.model.AdminLoginRequest;
import com.example.crossborder.model.AdminLoginResponse;
import com.example.crossborder.model.AdminMenu;
import com.example.crossborder.model.AdminProfile;
import com.example.crossborder.model.AdminRole;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.AdminUser;
import com.example.crossborder.model.CategoryConfig;
import com.example.crossborder.model.MarketConfig;
import com.example.crossborder.model.ScheduleConfig;
import com.example.crossborder.repository.AdminDataRepository;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.AdminSettingsService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminSettingsService settings;
    private final AdminDataRepository adminData;

    public AdminController(AdminAuthService auth, AdminSettingsService settings, AdminDataRepository adminData) {
        this.auth = auth;
        this.settings = settings;
        this.adminData = adminData;
    }

    @PostMapping("/login")
    public AdminLoginResponse login(@RequestBody AdminLoginRequest request) {
        if (!auth.login(request.username(), request.password())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        adminData.ensureSeedData();
        return new AdminLoginResponse(AdminAuthService.ADMIN_TOKEN, "admin");
    }

    @GetMapping("/profile")
    public AdminProfile profile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return new AdminProfile("admin", "系统管理员", List.of("admin"), List.of("*:*:*"));
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

    @GetMapping("/menus")
    public List<AdminMenu> menus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.menus();
    }

    @GetMapping("/users")
    public List<AdminUser> users(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.users();
    }

    @PostMapping("/users")
    public AdminUser createUser(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminUser user) {
        requireAuth(authorization);
        return adminData.saveUser(user);
    }

    @PutMapping("/users/{id}")
    public AdminUser updateUser(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody AdminUser user) {
        requireAuth(authorization);
        return adminData.saveUser(new AdminUser(id, user.username(), user.nickname(), user.roleKey(), user.status(), user.email()));
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        adminData.deleteUser(id);
        return Map.of("deleted", true);
    }

    @GetMapping("/roles")
    public List<AdminRole> roles(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.roles();
    }

    @PostMapping("/roles")
    public AdminRole createRole(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminRole role) {
        requireAuth(authorization);
        return adminData.saveRole(role);
    }

    @PutMapping("/roles/{id}")
    public AdminRole updateRole(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody AdminRole role) {
        requireAuth(authorization);
        return adminData.saveRole(new AdminRole(id, role.roleKey(), role.roleName(), role.status(), role.remark()));
    }

    @DeleteMapping("/roles/{id}")
    public Map<String, Object> deleteRole(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        adminData.deleteRole(id);
        return Map.of("deleted", true);
    }

    @GetMapping("/markets")
    public List<MarketConfig> markets(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.markets();
    }

    @PostMapping("/markets")
    public MarketConfig saveMarket(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody MarketConfig market) {
        requireAuth(authorization);
        return adminData.saveMarket(market);
    }

    @PutMapping("/markets/{id}")
    public MarketConfig updateMarket(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody MarketConfig market) {
        requireAuth(authorization);
        return adminData.saveMarket(new MarketConfig(id, market.marketKey(), market.marketName(), market.region(), market.enabled(), market.note()));
    }

    @GetMapping("/categories")
    public List<CategoryConfig> categories(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.categories();
    }

    @PostMapping("/categories")
    public CategoryConfig saveCategory(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody CategoryConfig category) {
        requireAuth(authorization);
        return adminData.saveCategory(category);
    }

    @PutMapping("/categories/{id}")
    public CategoryConfig updateCategory(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody CategoryConfig category) {
        requireAuth(authorization);
        return adminData.saveCategory(new CategoryConfig(id, category.categoryName(), category.marketKey(), category.enabled(), category.keywords(), category.note()));
    }

    @GetMapping("/schedules")
    public ScheduleConfig schedule(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.schedule();
    }

    private void requireAuth(String authorization) {
        if (!auth.authorized(authorization)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "admin login required");
    }
}

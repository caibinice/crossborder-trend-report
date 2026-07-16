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
import com.example.crossborder.model.SysConfig;
import com.example.crossborder.model.SysDictData;
import com.example.crossborder.model.SysDictType;
import com.example.crossborder.model.SysLoginLog;
import com.example.crossborder.model.SysOperLog;
import com.example.crossborder.model.SysTenant;
import com.example.crossborder.repository.AdminDataRepository;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.AdminInputValidator;
import com.example.crossborder.service.AdminSettingsService;
import com.example.crossborder.service.ApiValidationException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminSettingsService settings;
    private final AdminDataRepository adminData;
    private final AdminInputValidator validator;

    public AdminController(
        AdminAuthService auth,
        AdminSettingsService settings,
        AdminDataRepository adminData,
        AdminInputValidator validator
    ) {
        this.auth = auth;
        this.settings = settings;
        this.adminData = adminData;
        this.validator = validator;
    }

    @PostMapping("/login")
    public AdminLoginResponse login(@RequestBody AdminLoginRequest request, HttpServletRequest http) {
        if (request == null) {
            throw new ApiValidationException("登录参数不能为空");
        }
        validator.validateLogin(request.username(), request.password());
        adminData.ensureSeedData();
        boolean success = adminData.validateLogin(request.username(), request.password());
        adminData.logLogin(
            success ? adminData.tenantOf(request.username()) : AdminDataRepository.DEFAULT_TENANT,
            request.username(), clientIp(http), success ? "success" : "fail", success ? "登录成功" : "账号或密码错误"
        );
        if (!success) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return new AdminLoginResponse(auth.issue(request.username(), adminData.roleOf(request.username())), request.username());
    }

    @GetMapping("/auth/status")
    public Map<String, Object> authStatus() {
        return Map.of("enabled", auth.enabled());
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        HttpServletRequest http
    ) {
        requireAuth(authorization);
        AdminDataRepository.AdminActor current = actor(authorization);
        adminData.logOper(current.tenantId(), current.username(), "登录认证", "注销", "/logout", "success", "用户主动退出后台");
        adminData.logLogin(current.tenantId(), current.username(), clientIp(http), "success", "退出登录");
        return Map.of("loggedOut", true, "message", "已退出登录");
    }

    @GetMapping("/profile")
    public AdminProfile profile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.profile(actor(authorization).username());
    }

    @GetMapping("/settings")
    public AdminSettings getSettings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return settings.get();
    }

    @PutMapping("/settings")
    public AdminSettings saveSettings(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminSettings request) {
        requireAuth(authorization);
        validator.validateSettings(request);
        AdminSettings saved = settings.save(request);
        log(authorization, "系统配置", "保存", "/settings", "保存系统配置");
        return saved;
    }

    @GetMapping("/tenants")
    public List<SysTenant> tenants(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.tenants();
    }

    @PostMapping("/tenants")
    public SysTenant createTenant(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody SysTenant request) {
        requireAuth(authorization);
        validator.validateTenant(request);
        SysTenant saved = adminData.saveTenant(request);
        log(authorization, "租户管理", "新增", "/tenants", saved.tenantId());
        return saved;
    }

    @PutMapping("/tenants/{id}")
    public SysTenant updateTenant(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody SysTenant request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateTenant(request);
        SysTenant saved = adminData.saveTenant(new SysTenant(id, request.tenantId(), request.tenantName(), request.contactUser(), request.contactPhone(), request.packageName(), request.status(), request.remark()));
        log(authorization, "租户管理", "编辑", "/tenants/" + id, saved.tenantId());
        return saved;
    }

    @DeleteMapping("/tenants/{id}")
    public Map<String, Object> deleteTenant(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteTenant(id);
        log(authorization, "租户管理", "删除", "/tenants/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/users")
    public List<AdminUser> users(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.users(tenantId);
    }

    @PostMapping("/users")
    public AdminUser createUser(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminUser request) {
        requireAuth(authorization);
        validator.validateUser(request, true);
        AdminUser saved = adminData.saveUser(request);
        log(authorization, "用户管理", "新增", "/users", saved.username());
        return saved;
    }

    @PutMapping("/users/{id}")
    public AdminUser updateUser(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody AdminUser request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateUser(request, false);
        AdminUser saved = adminData.saveUser(new AdminUser(id, request.tenantId(), request.username(), request.password(), request.nickname(), request.roleKey(), request.status(), request.email(), request.phone()));
        log(authorization, "用户管理", "编辑", "/users/" + id, saved.username());
        return saved;
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteUser(id);
        log(authorization, "用户管理", "删除", "/users/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/roles")
    public List<AdminRole> roles(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.roles(tenantId);
    }

    @PostMapping("/roles")
    public AdminRole createRole(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminRole request) {
        requireAuth(authorization);
        validator.validateRole(request);
        AdminRole saved = adminData.saveRole(request);
        log(authorization, "角色管理", "新增", "/roles", saved.roleKey());
        return saved;
    }

    @PutMapping("/roles/{id}")
    public AdminRole updateRole(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody AdminRole request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateRole(request);
        AdminRole saved = adminData.saveRole(new AdminRole(id, request.tenantId(), request.roleKey(), request.roleName(), request.status(), request.menuKeys(), request.remark()));
        log(authorization, "角色管理", "编辑", "/roles/" + id, saved.roleKey());
        return saved;
    }

    @DeleteMapping("/roles/{id}")
    public Map<String, Object> deleteRole(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteRole(id);
        log(authorization, "角色管理", "删除", "/roles/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/menus")
    public List<AdminMenu> menus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.menusFor(actor(authorization).username());
    }

    @GetMapping("/menus/flat")
    public List<AdminMenu> flatMenus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAuth(authorization);
        return adminData.flatMenus();
    }

    @PostMapping("/menus")
    public AdminMenu createMenu(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AdminMenu request) {
        requireAuth(authorization);
        validator.validateMenu(request, false);
        AdminMenu saved = adminData.saveMenu(request);
        log(authorization, "菜单管理", "新增", "/menus", saved.menuKey());
        return saved;
    }

    @PutMapping("/menus/{id}")
    public AdminMenu updateMenu(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody AdminMenu request) {
        requireAuth(authorization);
        requireId(id);
        AdminMenu menu = new AdminMenu(id, request.parentId(), request.menuKey(), request.title(), request.icon(), request.path(), request.component(), request.permission(), request.sortOrder(), request.status(), List.of());
        validator.validateMenu(menu, true);
        AdminMenu saved = adminData.saveMenu(menu);
        log(authorization, "菜单管理", "编辑", "/menus/" + id, saved.menuKey());
        return saved;
    }

    @DeleteMapping("/menus/{id}")
    public Map<String, Object> deleteMenu(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteMenu(id);
        log(authorization, "菜单管理", "删除", "/menus/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/dict-types")
    public List<SysDictType> dictTypes(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.dictTypes(tenantId);
    }

    @PostMapping("/dict-types")
    public SysDictType createDictType(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody SysDictType request) {
        requireAuth(authorization);
        validator.validateDictType(request);
        SysDictType saved = adminData.saveDictType(request);
        log(authorization, "字典类型", "新增", "/dict-types", saved.dictType());
        return saved;
    }

    @PutMapping("/dict-types/{id}")
    public SysDictType updateDictType(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody SysDictType request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateDictType(request);
        SysDictType saved = adminData.saveDictType(new SysDictType(id, request.tenantId(), request.dictName(), request.dictType(), request.status(), request.remark()));
        log(authorization, "字典类型", "编辑", "/dict-types/" + id, saved.dictType());
        return saved;
    }

    @DeleteMapping("/dict-types/{id}")
    public Map<String, Object> deleteDictType(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteDictType(id);
        log(authorization, "字典类型", "删除", "/dict-types/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/dict-data")
    public List<SysDictData> dictData(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.dictData(tenantId);
    }

    @PostMapping("/dict-data")
    public SysDictData createDictData(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody SysDictData request) {
        requireAuth(authorization);
        validator.validateDictData(request);
        SysDictData saved = adminData.saveDictData(request);
        log(authorization, "字典数据", "新增", "/dict-data", saved.dictValue());
        return saved;
    }

    @PutMapping("/dict-data/{id}")
    public SysDictData updateDictData(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody SysDictData request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateDictData(request);
        SysDictData saved = adminData.saveDictData(new SysDictData(id, request.tenantId(), request.dictType(), request.dictLabel(), request.dictValue(), request.sortOrder(), request.status(), request.remark()));
        log(authorization, "字典数据", "编辑", "/dict-data/" + id, saved.dictValue());
        return saved;
    }

    @DeleteMapping("/dict-data/{id}")
    public Map<String, Object> deleteDictData(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteDictData(id);
        log(authorization, "字典数据", "删除", "/dict-data/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/configs")
    public List<SysConfig> configs(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.configs(tenantId);
    }

    @PostMapping("/configs")
    public SysConfig createConfig(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody SysConfig request) {
        requireAuth(authorization);
        validator.validateConfig(request);
        SysConfig saved = adminData.saveConfig(request);
        log(authorization, "参数配置", "新增", "/configs", saved.configKey());
        return saved;
    }

    @PutMapping("/configs/{id}")
    public SysConfig updateConfig(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody SysConfig request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateConfig(request);
        SysConfig saved = adminData.saveConfig(new SysConfig(id, request.tenantId(), request.configName(), request.configKey(), request.configValue(), request.systemBuiltin(), request.remark()));
        log(authorization, "参数配置", "编辑", "/configs/" + id, saved.configKey());
        return saved;
    }

    @DeleteMapping("/configs/{id}")
    public Map<String, Object> deleteConfig(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteConfig(id);
        log(authorization, "参数配置", "删除", "/configs/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/oper-logs")
    public List<SysOperLog> operLogs(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.operLogs(tenantId);
    }

    @GetMapping("/login-logs")
    public List<SysLoginLog> loginLogs(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.loginLogs(tenantId);
    }

    @GetMapping("/markets")
    public List<MarketConfig> markets(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.markets(tenantId);
    }

    @PostMapping("/markets")
    public MarketConfig createMarket(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody MarketConfig request) {
        requireAuth(authorization);
        validator.validateMarket(request);
        MarketConfig saved = adminData.saveMarket(request);
        log(authorization, "市场配置", "新增", "/markets", saved.marketKey());
        return saved;
    }

    @PutMapping("/markets/{id}")
    public MarketConfig updateMarket(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody MarketConfig request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateMarket(request);
        MarketConfig saved = adminData.saveMarket(new MarketConfig(id, request.tenantId(), request.marketKey(), request.marketName(), request.region(), request.enabled(), request.note()));
        log(authorization, "市场配置", "编辑", "/markets/" + id, saved.marketKey());
        return saved;
    }

    @DeleteMapping("/markets/{id}")
    public Map<String, Object> deleteMarket(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteMarket(id);
        log(authorization, "市场配置", "删除", "/markets/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/categories")
    public List<CategoryConfig> categories(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "*") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantFilter(tenantId);
        return adminData.categories(tenantId);
    }

    @PostMapping("/categories")
    public CategoryConfig createCategory(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody CategoryConfig request) {
        requireAuth(authorization);
        validator.validateCategory(request);
        CategoryConfig saved = adminData.saveCategory(request);
        log(authorization, "品类配置", "新增", "/categories", saved.categoryName());
        return saved;
    }

    @PutMapping("/categories/{id}")
    public CategoryConfig updateCategory(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id, @RequestBody CategoryConfig request) {
        requireAuth(authorization);
        requireId(id);
        validator.validateCategory(request);
        CategoryConfig saved = adminData.saveCategory(new CategoryConfig(id, request.tenantId(), request.categoryName(), request.marketKey(), request.enabled(), request.keywords(), request.note()));
        log(authorization, "品类配置", "编辑", "/categories/" + id, saved.categoryName());
        return saved;
    }

    @DeleteMapping("/categories/{id}")
    public Map<String, Object> deleteCategory(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long id) {
        requireAuth(authorization);
        requireId(id);
        adminData.deleteCategory(id);
        log(authorization, "品类配置", "删除", "/categories/" + id, String.valueOf(id));
        return Map.of("deleted", true);
    }

    @GetMapping("/schedules")
    public ScheduleConfig schedule(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(defaultValue = "default") String tenantId) {
        requireAuth(authorization);
        validator.validateTenantId(tenantId);
        return adminData.schedule(tenantId);
    }

    private void requireAuth(String authorization) {
        if (!auth.authorized(authorization)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后台");
        }
    }

    private void requireId(long id) {
        if (id <= 0) {
            throw new ApiValidationException("记录 ID 不合法");
        }
    }

    private AdminDataRepository.AdminActor actor(String authorization) {
        String username = auth.claims(authorization).map(claims -> claims.username()).orElse("admin");
        return adminData.actor(username).orElse(new AdminDataRepository.AdminActor(username, AdminDataRepository.DEFAULT_TENANT, "admin"));
    }

    private void log(String authorization, String module, String action, String method, String message) {
        AdminDataRepository.AdminActor actor = actor(authorization);
        adminData.logOper(actor.tenantId(), actor.username(), module, action, method, "success", message);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

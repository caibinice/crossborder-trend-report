
package com.example.crossborder.controller;

import com.example.crossborder.model.*;
import com.example.crossborder.repository.AdminDataRepository;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.AdminSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminSettingsService settings;
    private final AdminDataRepository adminData;
    public AdminController(AdminAuthService auth, AdminSettingsService settings, AdminDataRepository adminData) { this.auth = auth; this.settings = settings; this.adminData = adminData; }

    @PostMapping("/login")
    public AdminLoginResponse login(@RequestBody AdminLoginRequest request, HttpServletRequest http) {
        adminData.ensureSeedData();
        boolean ok = adminData.validateLogin(request.username(), request.password());
        adminData.logLogin(ok ? adminData.tenantOf(request.username()) : "default", request.username(), http.getRemoteAddr(), ok ? "success" : "fail", ok ? "登录成功" : "账号或密码错误");
        if (!ok) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        return new AdminLoginResponse(auth.issue(request.username(), "admin"), request.username());
    }

    @GetMapping("/auth/status") public Map<String,Object> authStatus(){ return Map.of("enabled", auth.enabled()); }
    @GetMapping("/profile") public AdminProfile profile(@RequestHeader(value="Authorization",required=false) String a){ requireAuth(a); return adminData.profile(auth.claims(a).map(c -> c.username()).orElse("admin")); }
    @GetMapping("/settings") public AdminSettings get(@RequestHeader(value="Authorization",required=false) String a){ requireAuth(a); return settings.get(); }
    @PutMapping("/settings") public AdminSettings save(@RequestHeader(value="Authorization",required=false) String a,@RequestBody AdminSettings r){ requireAuth(a); AdminSettings out=settings.save(r); log("系统配置","保存","/settings","success","保存系统配置"); return out; }

    @GetMapping("/tenants") public List<SysTenant> tenants(@RequestHeader(value="Authorization",required=false) String a){ requireAuth(a); return adminData.tenants(); }
    @PostMapping("/tenants") public SysTenant createTenant(@RequestHeader(value="Authorization",required=false) String a,@RequestBody SysTenant v){ requireAuth(a); SysTenant out=adminData.saveTenant(v); log("租户管理","新增","/tenants","success",out.tenantId()); return out; }
    @PutMapping("/tenants/{id}") public SysTenant updateTenant(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody SysTenant v){ requireAuth(a); SysTenant out=adminData.saveTenant(new SysTenant(id,v.tenantId(),v.tenantName(),v.contactUser(),v.contactPhone(),v.packageName(),v.status(),v.remark())); log("租户管理","编辑","/tenants/"+id,"success",out.tenantId()); return out; }
    @DeleteMapping("/tenants/{id}") public Map<String,Object> deleteTenant(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteTenant(id); log("租户管理","删除","/tenants/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/users") public List<AdminUser> users(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.users(tenantId); }
    @PostMapping("/users") public AdminUser createUser(@RequestHeader(value="Authorization",required=false) String a,@RequestBody AdminUser v){ requireAuth(a); AdminUser out=adminData.saveUser(v); log("用户管理","新增","/users","success",out.username()); return out; }
    @PutMapping("/users/{id}") public AdminUser updateUser(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody AdminUser v){ requireAuth(a); AdminUser out=adminData.saveUser(new AdminUser(id,v.tenantId(),v.username(),v.password(),v.nickname(),v.roleKey(),v.status(),v.email(),v.phone())); log("用户管理","编辑","/users/"+id,"success",out.username()); return out; }
    @DeleteMapping("/users/{id}") public Map<String,Object> deleteUser(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteUser(id); log("用户管理","删除","/users/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/roles") public List<AdminRole> roles(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.roles(tenantId); }
    @PostMapping("/roles") public AdminRole createRole(@RequestHeader(value="Authorization",required=false) String a,@RequestBody AdminRole v){ requireAuth(a); AdminRole out=adminData.saveRole(v); log("角色管理","新增","/roles","success",out.roleKey()); return out; }
    @PutMapping("/roles/{id}") public AdminRole updateRole(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody AdminRole v){ requireAuth(a); AdminRole out=adminData.saveRole(new AdminRole(id,v.tenantId(),v.roleKey(),v.roleName(),v.status(),v.menuKeys(),v.remark())); log("角色管理","编辑","/roles/"+id,"success",out.roleKey()); return out; }
    @DeleteMapping("/roles/{id}") public Map<String,Object> deleteRole(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteRole(id); log("角色管理","删除","/roles/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/menus") public List<AdminMenu> menus(@RequestHeader(value="Authorization",required=false) String a){ requireAuth(a); return adminData.menusFor(auth.claims(a).map(c -> c.username()).orElse("admin")); }
    @GetMapping("/menus/flat") public List<AdminMenu> flatMenus(@RequestHeader(value="Authorization",required=false) String a){ requireAuth(a); return adminData.flatMenus(); }
    @PostMapping("/menus") public AdminMenu createMenu(@RequestHeader(value="Authorization",required=false) String a,@RequestBody AdminMenu v){ requireAuth(a); AdminMenu out=adminData.saveMenu(v); log("菜单管理","新增","/menus","success",out.menuKey()); return out; }
    @PutMapping("/menus/{id}") public AdminMenu updateMenu(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody AdminMenu v){ requireAuth(a); AdminMenu out=adminData.saveMenu(new AdminMenu(id,v.parentId(),v.menuKey(),v.title(),v.icon(),v.path(),v.component(),v.permission(),v.sortOrder(),v.status(),List.of())); log("菜单管理","编辑","/menus/"+id,"success",out.menuKey()); return out; }
    @DeleteMapping("/menus/{id}") public Map<String,Object> deleteMenu(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteMenu(id); log("菜单管理","删除","/menus/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/dict-types") public List<SysDictType> dictTypes(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.dictTypes(tenantId); }
    @PostMapping("/dict-types") public SysDictType createDictType(@RequestHeader(value="Authorization",required=false) String a,@RequestBody SysDictType v){ requireAuth(a); SysDictType out=adminData.saveDictType(v); log("字典类型","新增","/dict-types","success",out.dictType()); return out; }
    @PutMapping("/dict-types/{id}") public SysDictType updateDictType(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody SysDictType v){ requireAuth(a); SysDictType out=adminData.saveDictType(new SysDictType(id,v.tenantId(),v.dictName(),v.dictType(),v.status(),v.remark())); log("字典类型","编辑","/dict-types/"+id,"success",out.dictType()); return out; }
    @DeleteMapping("/dict-types/{id}") public Map<String,Object> deleteDictType(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteDictType(id); log("字典类型","删除","/dict-types/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/dict-data") public List<SysDictData> dictData(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.dictData(tenantId); }
    @PostMapping("/dict-data") public SysDictData createDictData(@RequestHeader(value="Authorization",required=false) String a,@RequestBody SysDictData v){ requireAuth(a); SysDictData out=adminData.saveDictData(v); log("字典数据","新增","/dict-data","success",out.dictValue()); return out; }
    @PutMapping("/dict-data/{id}") public SysDictData updateDictData(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody SysDictData v){ requireAuth(a); SysDictData out=adminData.saveDictData(new SysDictData(id,v.tenantId(),v.dictType(),v.dictLabel(),v.dictValue(),v.sortOrder(),v.status(),v.remark())); log("字典数据","编辑","/dict-data/"+id,"success",out.dictValue()); return out; }
    @DeleteMapping("/dict-data/{id}") public Map<String,Object> deleteDictData(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteDictData(id); log("字典数据","删除","/dict-data/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/configs") public List<SysConfig> configs(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.configs(tenantId); }
    @PostMapping("/configs") public SysConfig createConfig(@RequestHeader(value="Authorization",required=false) String a,@RequestBody SysConfig v){ requireAuth(a); SysConfig out=adminData.saveConfig(v); log("参数配置","新增","/configs","success",out.configKey()); return out; }
    @PutMapping("/configs/{id}") public SysConfig updateConfig(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody SysConfig v){ requireAuth(a); SysConfig out=adminData.saveConfig(new SysConfig(id,v.tenantId(),v.configName(),v.configKey(),v.configValue(),v.systemBuiltin(),v.remark())); log("参数配置","编辑","/configs/"+id,"success",out.configKey()); return out; }
    @DeleteMapping("/configs/{id}") public Map<String,Object> deleteConfig(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id){ requireAuth(a); adminData.deleteConfig(id); log("参数配置","删除","/configs/"+id,"success",String.valueOf(id)); return Map.of("deleted",true); }

    @GetMapping("/oper-logs") public List<SysOperLog> operLogs(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.operLogs(tenantId); }
    @GetMapping("/login-logs") public List<SysLoginLog> loginLogs(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.loginLogs(tenantId); }

    @GetMapping("/markets") public List<MarketConfig> markets(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.markets(tenantId); }
    @PostMapping("/markets") public MarketConfig saveMarket(@RequestHeader(value="Authorization",required=false) String a,@RequestBody MarketConfig v){ requireAuth(a); MarketConfig out=adminData.saveMarket(v); log("市场配置","保存","/markets","success",out.marketKey()); return out; }
    @PutMapping("/markets/{id}") public MarketConfig updateMarket(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody MarketConfig v){ requireAuth(a); MarketConfig out=adminData.saveMarket(new MarketConfig(id,v.tenantId(),v.marketKey(),v.marketName(),v.region(),v.enabled(),v.note())); log("市场配置","编辑","/markets/"+id,"success",out.marketKey()); return out; }
    @GetMapping("/categories") public List<CategoryConfig> categories(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="*") String tenantId){ requireAuth(a); return adminData.categories(tenantId); }
    @PostMapping("/categories") public CategoryConfig saveCategory(@RequestHeader(value="Authorization",required=false) String a,@RequestBody CategoryConfig v){ requireAuth(a); CategoryConfig out=adminData.saveCategory(v); log("品类配置","保存","/categories","success",out.categoryName()); return out; }
    @PutMapping("/categories/{id}") public CategoryConfig updateCategory(@RequestHeader(value="Authorization",required=false) String a,@PathVariable long id,@RequestBody CategoryConfig v){ requireAuth(a); CategoryConfig out=adminData.saveCategory(new CategoryConfig(id,v.tenantId(),v.categoryName(),v.marketKey(),v.enabled(),v.keywords(),v.note())); log("品类配置","编辑","/categories/"+id,"success",out.categoryName()); return out; }
    @GetMapping("/schedules") public ScheduleConfig schedule(@RequestHeader(value="Authorization",required=false) String a,@RequestParam(defaultValue="default") String tenantId){ requireAuth(a); return adminData.schedule(tenantId); }

    private void requireAuth(String authorization) { if (!auth.authorized(authorization)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "admin login required"); }
    private void log(String module,String action,String method,String status,String message){ adminData.logOper("default","admin",module,action,method,status,message); }
}

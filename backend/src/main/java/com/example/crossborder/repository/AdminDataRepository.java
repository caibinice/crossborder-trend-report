package com.example.crossborder.repository;

import com.example.crossborder.config.BootstrapProperties;
import com.example.crossborder.config.SecurityProperties;
import com.example.crossborder.model.AdminMenu;
import com.example.crossborder.model.AdminProfile;
import com.example.crossborder.model.AdminRole;
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
import com.example.crossborder.service.ApiConflictException;
import com.example.crossborder.service.PasswordService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class AdminDataRepository {
    public static final String DEFAULT_TENANT = "default";
    private static final String BOOTSTRAP_KEY = "default-seed-v1";

    private final JdbcTemplate jdbc;
    private final AdminSettingsRepository settingsRepository;
    private final PasswordService passwords;
    private final BootstrapProperties bootstrapProperties;
    private final SecurityProperties securityProperties;
    private final TransactionTemplate transactions;

    public AdminDataRepository(
        JdbcTemplate jdbc,
        AdminSettingsRepository settingsRepository,
        PasswordService passwords,
        BootstrapProperties bootstrapProperties,
        SecurityProperties securityProperties,
        PlatformTransactionManager transactionManager
    ) {
        this.jdbc = jdbc;
        this.settingsRepository = settingsRepository;
        this.passwords = passwords;
        this.bootstrapProperties = bootstrapProperties;
        this.securityProperties = securityProperties;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * A database gets defaults at most once, and only when every business table is empty.
     * The marker is committed in the same transaction as the seed so later requests never
     * recreate data that an administrator intentionally deleted or changed.
     */
    public void ensureSeedData() {
        if (isBootstrapped()) {
            return;
        }
        transactions.executeWithoutResult(status -> initializeOnce());
    }

    private void initializeOnce() {
        if (isBootstrapped()) {
            return;
        }
        int claimed = jdbc.update("INSERT IGNORE INTO app_bootstrap_state(bootstrap_key) VALUES(?)", BOOTSTRAP_KEY);
        if (claimed == 0 || !isPristineDatabase()) {
            return;
        }
        seedDefaults();
    }

    private boolean isBootstrapped() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM app_bootstrap_state WHERE bootstrap_key=?", Integer.class, BOOTSTRAP_KEY);
        return count != null && count > 0;
    }

    private boolean isPristineDatabase() {
        return List.of(
            "trend_reports", "trend_products", "domestic_links", "admin_settings", "sys_tenant", "sys_role", "sys_user",
            "sys_menu", "sys_dict_type", "sys_dict_data", "sys_config", "sys_oper_log", "sys_login_log", "market_configs", "category_configs"
        ).stream().allMatch(table -> count(table) == 0);
    }

    private void seedDefaults() {
        jdbc.update("INSERT INTO sys_tenant(tenant_id,tenant_name,contact_user,contact_phone,package_name,status,remark) VALUES('default','默认租户','系统管理员','','基础套餐','enabled','跨境趋势报表默认租户')");
        jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES('default','admin','超级管理员','enabled','*','系统内置超级管理员')");
        jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES('default','tenant_admin','租户管理员','enabled','dashboard,selection,reports','租户侧配置与运营')");
        jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES('default','operator','运营人员','enabled','dashboard,reports','选品与报表查看')");
        jdbc.update("INSERT INTO sys_user(tenant_id,username,password,nickname,role_key,status,email,phone) VALUES('default','admin',?,'系统管理员','admin','enabled','admin@example.com','')", passwords.hash(initialAdminPassword()));
        seedMenus();
        jdbc.update("INSERT INTO sys_dict_type(tenant_id,dict_name,dict_type,status,remark) VALUES('default','系统状态','sys_normal_disable','enabled','启停通用状态')");
        jdbc.update("INSERT INTO sys_dict_type(tenant_id,dict_name,dict_type,status,remark) VALUES('default','市场区域','market_region','enabled','跨境市场区域字典')");
        jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','sys_normal_disable','正常','enabled',1,'enabled','')");
        jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','sys_normal_disable','停用','disabled',2,'enabled','')");
        jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','market_region','日本','jp',1,'enabled','')");
        jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','market_region','美国','us',2,'enabled','')");
        jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','market_region','东南亚','sea',3,'enabled','')");
        jdbc.update("INSERT INTO sys_config(tenant_id,config_name,config_key,config_value,system_builtin,remark) VALUES('default','系统名称','sys.name','跨境选品系统',true,'')");
        jdbc.update("INSERT INTO sys_config(tenant_id,config_name,config_key,config_value,system_builtin,remark) VALUES('default','默认语言','sys.lang','zh-CN',true,'')");
        jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES('default','jp','日本市场','日本',true,'已接入演示趋势数据，可继续接 TikTok/Amazon JP')");
        jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES('default','us','美国市场','美国',false,'待接入 Amazon US / TikTok US 数据源')");
        jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES('default','sea','东南亚市场','东南亚',false,'待接入 TikTok Shop SEA / Shopee / Lazada')");
        for (String category : List.of("玩具", "家居", "美妆", "宠物", "数码", "户外", "母婴", "汽车", "厨房", "文具", "服饰", "健康")) {
            jdbc.update("INSERT INTO category_configs(tenant_id,category_name,market_key,enabled,keywords,note) VALUES('default',?,'jp',true,?,'日本市场默认品类')", category, category);
        }
        settingsRepository.createDefaultIfMissing(DEFAULT_TENANT);
    }

    private String initialAdminPassword() {
        String configured = bootstrapProperties.initialAdminPassword();
        if (configured == null || configured.isBlank()) {
            if (securityProperties.enabled()) {
                throw new IllegalStateException("AUTH_ENABLED=true 时必须设置 INITIAL_ADMIN_PASSWORD");
            }
            return UUID.randomUUID().toString();
        }
        if (configured.length() < 8 || configured.length() > 72) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD 长度必须在 8 到 72 个字符之间");
        }
        return configured;
    }

    private int count(String table) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    private void seedMenus() {
        addMenu(1, 0, "dashboard", "首页 / 工作台", "dashboard", "/admin", "Dashboard", "dashboard:view", 1);
        addMenu(2, 0, "system", "系统管理", "system", "/admin/system", "Layout", "system:view", 10);
        addMenu(3, 2, "tenants", "租户管理", "tenant", "/admin/system/tenants", "TenantManage", "system:tenant:list", 11);
        addMenu(4, 2, "users", "用户管理", "user", "/admin/system/users", "UserManage", "system:user:list", 12);
        addMenu(5, 2, "roles", "角色管理", "peoples", "/admin/system/roles", "RoleManage", "system:role:list", 13);
        addMenu(6, 2, "menus", "菜单管理", "tree-table", "/admin/system/menus", "MenuManage", "system:menu:list", 14);
        addMenu(7, 2, "dict", "字典管理", "dict", "/admin/system/dict", "DictManage", "system:dict:list", 15);
        addMenu(8, 2, "configs", "参数配置", "config", "/admin/system/configs", "ConfigManage", "system:config:list", 16);
        addMenu(9, 2, "logs", "日志管理", "log", "/admin/system/logs", "LogManage", "system:log:list", 17);
        addMenu(10, 0, "selection", "选品配置", "shopping", "/admin/selection", "Layout", "selection:view", 20);
        addMenu(11, 10, "sources", "数据源配置", "link", "/admin/selection/sources", "SourceConfig", "selection:source:list", 21);
        addMenu(12, 10, "markets", "市场配置", "international", "/admin/selection/markets", "MarketConfig", "selection:market:list", 22);
        addMenu(13, 10, "categories", "品类配置", "category", "/admin/selection/categories", "CategoryConfig", "selection:category:list", 23);
        addMenu(14, 10, "schedules", "采集频率配置", "time", "/admin/selection/schedules", "ScheduleConfig", "selection:schedule:list", 24);
        addMenu(15, 0, "reports", "报表管理", "chart", "/admin/reports", "Layout", "reports:view", 30);
        addMenu(16, 15, "dailyReports", "日报记录", "date", "/admin/reports/daily", "DailyReports", "reports:daily:list", 31);
        addMenu(17, 15, "productPool", "商品池", "goods", "/admin/reports/products", "ProductPool", "reports:product:list", 32);
    }

    private void addMenu(long id, long parentId, String key, String title, String icon, String path, String component, String permission, int sort) {
        jdbc.update("INSERT INTO sys_menu(id,parent_id,menu_key,title,icon,path,component,permission,sort_order,status) VALUES(?,?,?,?,?,?,?,?,?,'enabled')", id, parentId, key, title, icon, path, component, permission, sort);
    }

    public AdminProfile profile(String username) {
        SysUserInfo user = userInfo(username);
        return new AdminProfile(user.username(), user.nickname(), user.tenantId(), List.of(user.roleKey()), permissions(user.tenantId(), user.roleKey()));
    }

    public boolean validateLogin(String username, String password) {
        ensureSeedData();
        List<StoredPassword> users = jdbc.query(
            "SELECT id,password FROM sys_user WHERE username=? AND status='enabled'",
            (rs, rowNum) -> new StoredPassword(rs.getLong("id"), s(rs, "password")),
            username
        );
        if (users.isEmpty() || !passwords.matches(password, users.get(0).password())) {
            return false;
        }
        StoredPassword user = users.get(0);
        if (passwords.needsUpgrade(user.password())) {
            jdbc.update("UPDATE sys_user SET password=? WHERE id=?", passwords.hash(password), user.id());
        }
        return true;
    }

    public String tenantOf(String username) {
        return userInfo(username).tenantId();
    }

    public String roleOf(String username) {
        return userInfo(username).roleKey();
    }

    public Optional<AdminActor> actor(String username) {
        try {
            SysUserInfo user = userInfo(username);
            return Optional.of(new AdminActor(user.username(), user.tenantId(), user.roleKey()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private SysUserInfo userInfo(String username) {
        ensureSeedData();
        return jdbc.query(
            "SELECT username,nickname,tenant_id,role_key FROM sys_user WHERE username=?",
            (rs, rowNum) -> new SysUserInfo(s(rs, "username"), s(rs, "nickname"), s(rs, "tenant_id"), s(rs, "role_key")),
            username
        ).stream().findFirst().orElseThrow(() -> new ApiConflictException("用户不存在或已被删除"));
    }

    private List<String> permissions(String tenantId, String roleKey) {
        List<String> menuKeys = jdbc.query("SELECT menu_keys FROM sys_role WHERE tenant_id=? AND role_key=?", (rs, rowNum) -> s(rs, 1), tenantId, roleKey);
        if (menuKeys.isEmpty() || "*".equals(menuKeys.get(0))) {
            return List.of("*:*");
        }
        return split(menuKeys.get(0));
    }

    private List<String> menuKeys(String tenantId, String roleKey) {
        List<String> values = jdbc.query("SELECT menu_keys FROM sys_role WHERE tenant_id=? AND role_key=?", (rs, rowNum) -> s(rs, 1), tenantId, roleKey);
        return values.isEmpty() ? List.of() : split(values.get(0));
    }

    private record SysUserInfo(String username, String nickname, String tenantId, String roleKey) {}
    private record StoredPassword(long id, String password) {}
    public record AdminActor(String username, String tenantId, String roleKey) {}

    public List<SysTenant> tenants() {
        ensureSeedData();
        return jdbc.query("SELECT * FROM sys_tenant ORDER BY id", tenantMapper());
    }

    public SysTenant saveTenant(SysTenant tenant) {
        ensureSeedData();
        if (tenant.id() > 0) {
            SysTenant existing = findTenant(tenant.id());
            if (!existing.tenantId().equals(tenant.tenantId())) {
                throw new ApiConflictException("已有数据的租户标识不能修改");
            }
            jdbc.update("UPDATE sys_tenant SET tenant_id=?,tenant_name=?,contact_user=?,contact_phone=?,package_name=?,status=?,remark=? WHERE id=?", tenant.tenantId(), tenant.tenantName(), tenant.contactUser(), tenant.contactPhone(), tenant.packageName(), tenant.status(), tenant.remark(), tenant.id());
        } else {
            jdbc.update("INSERT INTO sys_tenant(tenant_id,tenant_name,contact_user,contact_phone,package_name,status,remark) VALUES(?,?,?,?,?,?,?)", tenant.tenantId(), tenant.tenantName(), tenant.contactUser(), tenant.contactPhone(), tenant.packageName(), tenant.status(), tenant.remark());
            settingsRepository.createDefaultIfMissing(tenant.tenantId());
        }
        return tenants().stream().filter(item -> item.tenantId().equals(tenant.tenantId())).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteTenant(long id) {
        SysTenant tenant = findTenant(id);
        if (DEFAULT_TENANT.equals(tenant.tenantId())) {
            throw new ApiConflictException("默认租户不能删除");
        }
        for (String table : List.of(
            "trend_reports", "trend_products", "domestic_links", "admin_settings", "sys_user", "sys_role", "sys_dict_type",
            "sys_dict_data", "sys_config", "sys_oper_log", "sys_login_log", "market_configs", "category_configs", "report_collection_locks"
        )) {
            requireNoReferences("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?", tenant.tenantId(), "该租户仍有关联数据，不能删除");
        }
        jdbc.update("DELETE FROM sys_tenant WHERE id=?", id);
    }

    public List<AdminUser> users(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_user", "id", tenantId, (rs, rowNum) -> new AdminUser(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "username"), "", s(rs, "nickname"), s(rs, "role_key"), s(rs, "status"), s(rs, "email"), s(rs, "phone")));
    }

    public AdminUser saveUser(AdminUser user) {
        ensureSeedData();
        assertRoleExists(def(user.tenantId()), user.roleKey());
        if (user.id() > 0) {
            AdminUser existing = findUser(user.id());
            if (!existing.tenantId().equals(def(user.tenantId()))) {
                throw new ApiConflictException("已有用户不能直接切换租户");
            }
            if (user.password() == null || user.password().isBlank()) {
                jdbc.update("UPDATE sys_user SET tenant_id=?,username=?,nickname=?,role_key=?,status=?,email=?,phone=? WHERE id=?", def(user.tenantId()), user.username(), user.nickname(), user.roleKey(), user.status(), user.email(), user.phone(), user.id());
            } else {
                jdbc.update("UPDATE sys_user SET tenant_id=?,username=?,password=?,nickname=?,role_key=?,status=?,email=?,phone=? WHERE id=?", def(user.tenantId()), user.username(), passwords.hash(user.password()), user.nickname(), user.roleKey(), user.status(), user.email(), user.phone(), user.id());
            }
        } else {
            jdbc.update("INSERT INTO sys_user(tenant_id,username,password,nickname,role_key,status,email,phone) VALUES(?,?,?,?,?,?,?,?)", def(user.tenantId()), user.username(), passwords.hash(user.password()), user.nickname(), user.roleKey(), user.status(), user.email(), user.phone());
        }
        return users("*").stream().filter(item -> item.username().equals(user.username())).findFirst().orElseThrow();
    }

    public void deleteUser(long id) {
        AdminUser user = findUser(id);
        if ("admin".equals(user.username())) {
            throw new ApiConflictException("默认管理员不能删除");
        }
        jdbc.update("DELETE FROM sys_user WHERE id=?", id);
    }

    public List<AdminRole> roles(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_role", "id", tenantId, (rs, rowNum) -> new AdminRole(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "role_key"), s(rs, "role_name"), s(rs, "status"), split(s(rs, "menu_keys")), s(rs, "remark")));
    }

    public AdminRole saveRole(AdminRole role) {
        ensureSeedData();
        assertMenusExist(role.menuKeys());
        if (role.id() > 0) {
            AdminRole existing = findRole(role.id());
            if (!existing.tenantId().equals(def(role.tenantId()))) {
                throw new ApiConflictException("已有角色不能直接切换租户");
            }
            if (!existing.roleKey().equals(role.roleKey())) {
                requireNoReferences("SELECT COUNT(*) FROM sys_user WHERE tenant_id=? AND role_key=?", existing.tenantId(), existing.roleKey(), "该角色仍被用户使用，不能修改角色标识");
            }
            jdbc.update("UPDATE sys_role SET tenant_id=?,role_key=?,role_name=?,status=?,menu_keys=?,remark=? WHERE id=?", def(role.tenantId()), role.roleKey(), role.roleName(), role.status(), join(role.menuKeys()), role.remark(), role.id());
        } else {
            jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES(?,?,?,?,?,?)", def(role.tenantId()), role.roleKey(), role.roleName(), role.status(), join(role.menuKeys()), role.remark());
        }
        return roles("*").stream().filter(item -> item.roleKey().equals(role.roleKey()) && item.tenantId().equals(def(role.tenantId()))).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteRole(long id) {
        AdminRole role = findRole(id);
        if ("admin".equals(role.roleKey())) {
            throw new ApiConflictException("系统管理员角色不能删除");
        }
        requireNoReferences("SELECT COUNT(*) FROM sys_user WHERE tenant_id=? AND role_key=?", role.tenantId(), role.roleKey(), "该角色仍被用户使用，不能删除");
        jdbc.update("DELETE FROM sys_role WHERE id=?", id);
    }

    public List<AdminMenu> menus() {
        ensureSeedData();
        return tree(flatMenus(), 0);
    }

    public List<AdminMenu> menusFor(String username) {
        SysUserInfo user = userInfo(username);
        List<String> allowed = menuKeys(user.tenantId(), user.roleKey());
        if (allowed.contains("*")) {
            return menus();
        }
        List<AdminMenu> flat = flatMenus();
        Map<Long, AdminMenu> byId = flat.stream().collect(Collectors.toMap(AdminMenu::id, item -> item));
        Set<Long> ids = new HashSet<>();
        for (AdminMenu menu : flat) {
            if (!allowed.contains(menu.menuKey())) {
                continue;
            }
            ids.add(menu.id());
            long parent = menu.parentId();
            while (parent != 0) {
                ids.add(parent);
                AdminMenu parentMenu = byId.get(parent);
                parent = parentMenu == null ? 0 : parentMenu.parentId();
            }
        }
        return tree(flat.stream().filter(menu -> ids.contains(menu.id())).toList(), 0);
    }

    public List<AdminMenu> flatMenus() {
        ensureSeedData();
        return jdbc.query("SELECT * FROM sys_menu ORDER BY sort_order,id", menuMapper());
    }

    public AdminMenu saveMenu(AdminMenu menu) {
        ensureSeedData();
        assertValidParent(menu);
        if (menu.id() > 0) {
            AdminMenu existing = findMenu(menu.id());
            if (!existing.menuKey().equals(menu.menuKey())) {
                Integer roleCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE menu_keys <> '*' AND FIND_IN_SET(?, menu_keys) > 0", Integer.class, existing.menuKey());
                if (roleCount != null && roleCount > 0) {
                    throw new ApiConflictException("该菜单仍被角色授权，不能修改菜单标识");
                }
            }
            jdbc.update("UPDATE sys_menu SET parent_id=?,menu_key=?,title=?,icon=?,path=?,component=?,permission=?,sort_order=?,status=? WHERE id=?", menu.parentId(), menu.menuKey(), menu.title(), menu.icon(), menu.path(), menu.component(), menu.permission(), menu.sortOrder(), menu.status(), menu.id());
        } else {
            jdbc.update("INSERT INTO sys_menu(parent_id,menu_key,title,icon,path,component,permission,sort_order,status) VALUES(?,?,?,?,?,?,?,?,?)", menu.parentId(), menu.menuKey(), menu.title(), menu.icon(), menu.path(), menu.component(), menu.permission(), menu.sortOrder(), menu.status());
        }
        return flatMenus().stream().filter(item -> item.menuKey().equals(menu.menuKey())).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteMenu(long id) {
        AdminMenu menu = findMenu(id);
        requireNoReferences("SELECT COUNT(*) FROM sys_menu WHERE parent_id=?", id, "该菜单仍有子菜单，不能删除");
        Integer roleCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE menu_keys <> '*' AND FIND_IN_SET(?, menu_keys) > 0", Integer.class, menu.menuKey());
        if (roleCount != null && roleCount > 0) {
            throw new ApiConflictException("该菜单仍被角色授权，不能删除");
        }
        jdbc.update("DELETE FROM sys_menu WHERE id=?", id);
    }

    public List<SysDictType> dictTypes(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_dict_type", "id", tenantId, (rs, rowNum) -> new SysDictType(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "dict_name"), s(rs, "dict_type"), s(rs, "status"), s(rs, "remark")));
    }

    public SysDictType saveDictType(SysDictType type) {
        if (type.id() > 0) {
            SysDictType existing = findDictType(type.id());
            if (!existing.tenantId().equals(def(type.tenantId()))) {
                throw new ApiConflictException("已有字典类型不能直接切换租户");
            }
            if (!existing.dictType().equals(type.dictType())) {
                requireNoReferences("SELECT COUNT(*) FROM sys_dict_data WHERE tenant_id=? AND dict_type=?", existing.tenantId(), existing.dictType(), "该字典类型仍有字典数据，不能修改字典类型标识");
            }
            jdbc.update("UPDATE sys_dict_type SET tenant_id=?,dict_name=?,dict_type=?,status=?,remark=? WHERE id=?", def(type.tenantId()), type.dictName(), type.dictType(), type.status(), type.remark(), type.id());
        } else {
            jdbc.update("INSERT INTO sys_dict_type(tenant_id,dict_name,dict_type,status,remark) VALUES(?,?,?,?,?)", def(type.tenantId()), type.dictName(), type.dictType(), type.status(), type.remark());
        }
        return dictTypes("*").stream().filter(item -> item.dictType().equals(type.dictType()) && item.tenantId().equals(def(type.tenantId()))).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteDictType(long id) {
        SysDictType type = findDictType(id);
        requireNoReferences("SELECT COUNT(*) FROM sys_dict_data WHERE tenant_id=? AND dict_type=?", type.tenantId(), type.dictType(), "该字典类型仍有字典数据，不能删除");
        jdbc.update("DELETE FROM sys_dict_type WHERE id=?", id);
    }

    public List<SysDictData> dictData(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_dict_data", "dict_type,sort_order,id", tenantId, (rs, rowNum) -> new SysDictData(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "dict_type"), s(rs, "dict_label"), s(rs, "dict_value"), rs.getInt("sort_order"), s(rs, "status"), s(rs, "remark")));
    }

    public SysDictData saveDictData(SysDictData data) {
        assertDictTypeExists(def(data.tenantId()), data.dictType());
        if (data.id() > 0) {
            SysDictData existing = findDictData(data.id());
            if (!existing.tenantId().equals(def(data.tenantId()))) {
                throw new ApiConflictException("已有字典数据不能直接切换租户");
            }
            jdbc.update("UPDATE sys_dict_data SET tenant_id=?,dict_type=?,dict_label=?,dict_value=?,sort_order=?,status=?,remark=? WHERE id=?", def(data.tenantId()), data.dictType(), data.dictLabel(), data.dictValue(), data.sortOrder(), data.status(), data.remark(), data.id());
        } else {
            jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES(?,?,?,?,?,?,?)", def(data.tenantId()), data.dictType(), data.dictLabel(), data.dictValue(), data.sortOrder(), data.status(), data.remark());
        }
        return dictData("*").stream().filter(item -> item.dictType().equals(data.dictType()) && item.dictValue().equals(data.dictValue()) && item.tenantId().equals(def(data.tenantId()))).findFirst().orElseThrow();
    }

    public void deleteDictData(long id) {
        findDictData(id);
        jdbc.update("DELETE FROM sys_dict_data WHERE id=?", id);
    }

    public List<SysConfig> configs(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_config", "id", tenantId, (rs, rowNum) -> new SysConfig(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "config_name"), s(rs, "config_key"), s(rs, "config_value"), rs.getBoolean("system_builtin"), s(rs, "remark")));
    }

    public SysConfig saveConfig(SysConfig config) {
        if (config.id() > 0) {
            SysConfig existing = findConfig(config.id());
            if (!existing.tenantId().equals(def(config.tenantId()))) {
                throw new ApiConflictException("已有参数不能直接切换租户");
            }
            jdbc.update("UPDATE sys_config SET tenant_id=?,config_name=?,config_key=?,config_value=?,system_builtin=?,remark=? WHERE id=?", def(config.tenantId()), config.configName(), config.configKey(), config.configValue(), config.systemBuiltin(), config.remark(), config.id());
        } else {
            jdbc.update("INSERT INTO sys_config(tenant_id,config_name,config_key,config_value,system_builtin,remark) VALUES(?,?,?,?,?,?)", def(config.tenantId()), config.configName(), config.configKey(), config.configValue(), config.systemBuiltin(), config.remark());
        }
        return configs("*").stream().filter(item -> item.configKey().equals(config.configKey()) && item.tenantId().equals(def(config.tenantId()))).findFirst().orElseThrow();
    }

    public void deleteConfig(long id) {
        SysConfig config = findConfig(id);
        if (config.systemBuiltin()) {
            throw new ApiConflictException("系统内置参数不能删除");
        }
        jdbc.update("DELETE FROM sys_config WHERE id=?", id);
    }

    public List<MarketConfig> markets(String tenantId) {
        ensureSeedData();
        return listByTenant("market_configs", "id", tenantId, (rs, rowNum) -> new MarketConfig(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "market_key"), s(rs, "market_name"), s(rs, "region"), rs.getBoolean("enabled"), s(rs, "note")));
    }

    public MarketConfig saveMarket(MarketConfig market) {
        ensureSeedData();
        if (market.id() > 0) {
            MarketConfig existing = findMarket(market.id());
            if (!existing.tenantId().equals(def(market.tenantId()))) {
                throw new ApiConflictException("已有市场不能直接切换租户");
            }
            if (!existing.marketKey().equals(market.marketKey())) {
                requireNoReferences("SELECT COUNT(*) FROM category_configs WHERE tenant_id=? AND market_key=?", existing.tenantId(), existing.marketKey(), "该市场仍有关联品类，不能修改市场标识");
            }
            jdbc.update("UPDATE market_configs SET tenant_id=?,market_key=?,market_name=?,region=?,enabled=?,note=? WHERE id=?", def(market.tenantId()), market.marketKey(), market.marketName(), market.region(), market.enabled(), market.note(), market.id());
        } else {
            jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES(?,?,?,?,?,?)", def(market.tenantId()), market.marketKey(), market.marketName(), market.region(), market.enabled(), market.note());
        }
        return markets("*").stream().filter(item -> item.marketKey().equals(market.marketKey()) && item.tenantId().equals(def(market.tenantId()))).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteMarket(long id) {
        MarketConfig market = findMarket(id);
        requireNoReferences("SELECT COUNT(*) FROM category_configs WHERE tenant_id=? AND market_key=?", market.tenantId(), market.marketKey(), "该市场仍有关联品类，不能删除");
        jdbc.update("DELETE FROM market_configs WHERE id=?", id);
    }

    public List<CategoryConfig> categories(String tenantId) {
        ensureSeedData();
        return listByTenant("category_configs", "id", tenantId, (rs, rowNum) -> new CategoryConfig(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "category_name"), s(rs, "market_key"), rs.getBoolean("enabled"), s(rs, "keywords"), s(rs, "note")));
    }

    public CategoryConfig saveCategory(CategoryConfig category) {
        ensureSeedData();
        assertMarketExists(def(category.tenantId()), category.marketKey());
        if (category.id() > 0) {
            CategoryConfig existing = findCategory(category.id());
            if (!existing.tenantId().equals(def(category.tenantId()))) {
                throw new ApiConflictException("已有品类不能直接切换租户");
            }
            jdbc.update("UPDATE category_configs SET tenant_id=?,category_name=?,market_key=?,enabled=?,keywords=?,note=? WHERE id=?", def(category.tenantId()), category.categoryName(), category.marketKey(), category.enabled(), category.keywords(), category.note(), category.id());
        } else {
            jdbc.update("INSERT INTO category_configs(tenant_id,category_name,market_key,enabled,keywords,note) VALUES(?,?,?,?,?,?)", def(category.tenantId()), category.categoryName(), category.marketKey(), category.enabled(), category.keywords(), category.note());
        }
        return categories("*").stream().filter(item -> item.categoryName().equals(category.categoryName()) && item.tenantId().equals(def(category.tenantId()))).findFirst().orElseThrow();
    }

    public void deleteCategory(long id) {
        findCategory(id);
        jdbc.update("DELETE FROM category_configs WHERE id=?", id);
    }

    public ScheduleConfig schedule(String tenantId) {
        var settings = settingsRepository.get(def(tenantId));
        return new ScheduleConfig(settings.frequencyCron(), settings.maxProducts(), settings.jpyCnyRate(), settings.defaultShippingCny(), settings.smartMode());
    }

    public void logOper(String tenantId, String username, String module, String action, String method, String status, String message) {
        jdbc.update("INSERT INTO sys_oper_log(tenant_id,username,module,action,method,status,message) VALUES(?,?,?,?,?,?,?)", def(tenantId), username, module, action, method, status, message);
    }

    public void logLogin(String tenantId, String username, String ipaddr, String status, String message) {
        jdbc.update("INSERT INTO sys_login_log(tenant_id,username,ipaddr,status,message) VALUES(?,?,?,?,?)", def(tenantId), username, ipaddr, status, message);
    }

    public List<SysOperLog> operLogs(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_oper_log", "id DESC LIMIT 200", tenantId, (rs, rowNum) -> new SysOperLog(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "username"), s(rs, "module"), s(rs, "action"), s(rs, "method"), s(rs, "status"), s(rs, "message"), instant(rs.getTimestamp("created_at"))));
    }

    public List<SysLoginLog> loginLogs(String tenantId) {
        ensureSeedData();
        return listByTenant("sys_login_log", "id DESC LIMIT 200", tenantId, (rs, rowNum) -> new SysLoginLog(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "username"), s(rs, "ipaddr"), s(rs, "status"), s(rs, "message"), instant(rs.getTimestamp("created_at"))));
    }

    private <T> List<T> listByTenant(String table, String orderBy, String tenantId, RowMapper<T> mapper) {
        if ("*".equals(tenantId)) {
            return jdbc.query("SELECT * FROM " + table + " ORDER BY " + orderBy, mapper);
        }
        return jdbc.query("SELECT * FROM " + table + " WHERE tenant_id=? ORDER BY " + orderBy, mapper, tenantId);
    }

    private List<AdminMenu> tree(List<AdminMenu> flat, long parentId) {
        List<AdminMenu> result = new ArrayList<>();
        for (AdminMenu menu : flat) {
            if (menu.parentId() == parentId) {
                result.add(new AdminMenu(menu.id(), menu.parentId(), menu.menuKey(), menu.title(), menu.icon(), menu.path(), menu.component(), menu.permission(), menu.sortOrder(), menu.status(), tree(flat, menu.id())));
            }
        }
        return result;
    }

    private void assertRoleExists(String tenantId, String roleKey) {
        requireExists("SELECT COUNT(*) FROM sys_role WHERE tenant_id=? AND role_key=?", new Object[]{tenantId, roleKey}, "角色不存在");
    }

    private void assertDictTypeExists(String tenantId, String dictType) {
        requireExists("SELECT COUNT(*) FROM sys_dict_type WHERE tenant_id=? AND dict_type=?", new Object[]{tenantId, dictType}, "字典类型不存在");
    }

    private void assertMarketExists(String tenantId, String marketKey) {
        requireExists("SELECT COUNT(*) FROM market_configs WHERE tenant_id=? AND market_key=?", new Object[]{tenantId, marketKey}, "市场不存在");
    }

    private void assertMenusExist(List<String> menuKeys) {
        List<String> keys = menuKeys == null ? List.of() : menuKeys.stream().filter(key -> key != null && !key.isBlank()).distinct().toList();
        if (keys.isEmpty() || keys.contains("*")) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(keys.size(), "?"));
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE menu_key IN (" + placeholders + ")", Integer.class, keys.toArray());
        if (count == null || count != keys.size()) {
            throw new ApiConflictException("角色包含不存在的菜单权限");
        }
    }

    private void assertValidParent(AdminMenu menu) {
        if (menu.parentId() == 0) {
            return;
        }
        AdminMenu parent = findMenu(menu.parentId());
        if (menu.id() > 0 && isDescendant(menu.id(), parent.id())) {
            throw new ApiConflictException("菜单不能移动到自身的子菜单下");
        }
    }

    private boolean isDescendant(long menuId, long candidateParentId) {
        long current = candidateParentId;
        Set<Long> visited = new HashSet<>();
        while (current != 0 && visited.add(current)) {
            if (current == menuId) {
                return true;
            }
            current = jdbc.query("SELECT parent_id FROM sys_menu WHERE id=?", (rs, rowNum) -> rs.getLong(1), current).stream().findFirst().orElse(0L);
        }
        return false;
    }

    private void requireNoReferences(String sql, Object argument, String message) {
        requireNoReferences(sql, new Object[]{argument}, message);
    }

    private void requireNoReferences(String sql, Object first, Object second, String message) {
        requireNoReferences(sql, new Object[]{first, second}, message);
    }

    private void requireNoReferences(String sql, Object[] args, String message) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        if (count != null && count > 0) {
            throw new ApiConflictException(message);
        }
    }

    private void requireExists(String sql, Object[] args, String message) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        if (count == null || count == 0) {
            throw new ApiConflictException(message);
        }
    }

    private SysTenant findTenant(long id) {
        return jdbc.query("SELECT * FROM sys_tenant WHERE id=?", tenantMapper(), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("租户不存在"));
    }

    private AdminRole findRole(long id) {
        return jdbc.query("SELECT * FROM sys_role WHERE id=?", (rs, rowNum) -> new AdminRole(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "role_key"), s(rs, "role_name"), s(rs, "status"), split(s(rs, "menu_keys")), s(rs, "remark")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("角色不存在"));
    }

    private AdminUser findUser(long id) {
        return jdbc.query("SELECT * FROM sys_user WHERE id=?", (rs, rowNum) -> new AdminUser(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "username"), "", s(rs, "nickname"), s(rs, "role_key"), s(rs, "status"), s(rs, "email"), s(rs, "phone")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("用户不存在"));
    }

    private AdminMenu findMenu(long id) {
        return jdbc.query("SELECT * FROM sys_menu WHERE id=?", menuMapper(), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("菜单不存在"));
    }

    private SysDictType findDictType(long id) {
        return jdbc.query("SELECT * FROM sys_dict_type WHERE id=?", (rs, rowNum) -> new SysDictType(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "dict_name"), s(rs, "dict_type"), s(rs, "status"), s(rs, "remark")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("字典类型不存在"));
    }

    private SysDictData findDictData(long id) {
        return jdbc.query("SELECT * FROM sys_dict_data WHERE id=?", (rs, rowNum) -> new SysDictData(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "dict_type"), s(rs, "dict_label"), s(rs, "dict_value"), rs.getInt("sort_order"), s(rs, "status"), s(rs, "remark")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("字典数据不存在"));
    }

    private MarketConfig findMarket(long id) {
        return jdbc.query("SELECT * FROM market_configs WHERE id=?", (rs, rowNum) -> new MarketConfig(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "market_key"), s(rs, "market_name"), s(rs, "region"), rs.getBoolean("enabled"), s(rs, "note")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("市场不存在"));
    }

    private CategoryConfig findCategory(long id) {
        return jdbc.query("SELECT * FROM category_configs WHERE id=?", (rs, rowNum) -> new CategoryConfig(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "category_name"), s(rs, "market_key"), rs.getBoolean("enabled"), s(rs, "keywords"), s(rs, "note")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("品类不存在"));
    }

    private SysConfig findConfig(long id) {
        return jdbc.query("SELECT * FROM sys_config WHERE id=?", (rs, rowNum) -> new SysConfig(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "config_name"), s(rs, "config_key"), s(rs, "config_value"), rs.getBoolean("system_builtin"), s(rs, "remark")), id).stream().findFirst().orElseThrow(() -> new ApiConflictException("参数不存在"));
    }

    private RowMapper<SysTenant> tenantMapper() {
        return (rs, rowNum) -> new SysTenant(rs.getLong("id"), s(rs, "tenant_id"), s(rs, "tenant_name"), s(rs, "contact_user"), s(rs, "contact_phone"), s(rs, "package_name"), s(rs, "status"), s(rs, "remark"));
    }

    private RowMapper<AdminMenu> menuMapper() {
        return (rs, rowNum) -> new AdminMenu(rs.getLong("id"), rs.getLong("parent_id"), s(rs, "menu_key"), s(rs, "title"), s(rs, "icon"), s(rs, "path"), s(rs, "component"), s(rs, "permission"), rs.getInt("sort_order"), s(rs, "status"), List.of());
    }

    private String def(String value) {
        return value == null || value.isBlank() ? DEFAULT_TENANT : value;
    }

    private List<String> split(String value) {
        return Arrays.stream((value == null ? "" : value).split(",")).map(String::trim).filter(item -> !item.isBlank()).collect(Collectors.toList());
    }

    private String join(List<String> values) {
        return values == null ? "" : values.stream().map(String::trim).filter(value -> !value.isBlank()).collect(Collectors.joining(","));
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }

    private String s(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }

    private String s(ResultSet rs, int index) throws SQLException {
        return rs.getString(index);
    }
}

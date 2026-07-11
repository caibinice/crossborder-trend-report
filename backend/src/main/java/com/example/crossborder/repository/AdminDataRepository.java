package com.example.crossborder.repository;

import com.example.crossborder.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminDataRepository {
    public static final String DEFAULT_TENANT = "default";
    private final JdbcTemplate jdbc;
    private final AdminSettingsRepository settingsRepository;
    public AdminDataRepository(JdbcTemplate jdbc, AdminSettingsRepository settingsRepository) { this.jdbc = jdbc; this.settingsRepository = settingsRepository; }

    public void ensureSeedData() {
        ensureTenantColumns();
        if (count("sys_tenant") == 0) {
            jdbc.update("INSERT INTO sys_tenant(tenant_id,tenant_name,contact_user,contact_phone,package_name,status,remark) VALUES('default','默认租户','系统管理员','','基础套餐','enabled','跨境趋势报表默认租户')");
        }
        if (count("sys_role") == 0) {
            jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES('default','admin','超级管理员','enabled','*','系统内置超级管理员')");
            jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES('default','tenant_admin','租户管理员','enabled','dashboard,selection,reports','租户侧配置与运营')");
            jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES('default','operator','运营人员','enabled','dashboard,reports','选品与报表查看')");
        }
        if (count("sys_user") == 0) {
            jdbc.update("INSERT INTO sys_user(tenant_id,username,password,nickname,role_key,status,email,phone) VALUES('default','admin','admin','系统管理员','admin','enabled','admin@example.com','')");
        }
        if (count("sys_menu") == 0) seedMenus();
        if (count("sys_dict_type") == 0) {
            jdbc.update("INSERT INTO sys_dict_type(tenant_id,dict_name,dict_type,status,remark) VALUES('default','系统状态','sys_normal_disable','enabled','启停通用状态')");
            jdbc.update("INSERT INTO sys_dict_type(tenant_id,dict_name,dict_type,status,remark) VALUES('default','市场区域','market_region','enabled','跨境市场区域字典')");
            jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','sys_normal_disable','正常','enabled',1,'enabled','')");
            jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','sys_normal_disable','停用','disabled',2,'enabled','')");
            jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','market_region','日本','jp',1,'enabled','')");
            jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','market_region','美国','us',2,'enabled','')");
            jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES('default','market_region','东南亚','sea',3,'enabled','')");
        }
        if (count("sys_config") == 0) {
            jdbc.update("INSERT INTO sys_config(tenant_id,config_name,config_key,config_value,system_builtin,remark) VALUES('default','系统名称','sys.name','跨境选品系统',true,'')");
            jdbc.update("INSERT INTO sys_config(tenant_id,config_name,config_key,config_value,system_builtin,remark) VALUES('default','默认语言','sys.lang','zh-CN',true,'')");
        }
        if (count("market_configs") == 0) {
            jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES('default','jp','日本市场','日本',true,'已接入演示趋势数据，可继续接 TikTok/Amazon JP')");
            jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES('default','us','美国市场','美国',false,'待接入 Amazon US / TikTok US 数据源')");
            jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES('default','sea','东南亚市场','东南亚',false,'待接入 TikTok Shop SEA / Shopee / Lazada')");
        }
        if (count("category_configs") == 0) {
            for (String c : List.of("玩具","家居","美妆","宠物","数码","户外","母婴","汽车","厨房","文具","服饰","健康")) {
                jdbc.update("INSERT INTO category_configs(tenant_id,category_name,market_key,enabled,keywords,note) VALUES('default',?,'jp',true,?,'日本市场默认品类')", c, c);
            }
        }
        settingsRepository.ensureDefault(DEFAULT_TENANT);
    }

    private void ensureTenantColumns() {
        for (String table : List.of("trend_reports","trend_products","domestic_links","admin_settings","market_configs","category_configs")) {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'tenant_id'", Integer.class, table);
            if (exists == null || exists == 0) jdbc.execute("ALTER TABLE " + table + " ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default'");
            jdbc.execute("UPDATE " + table + " SET tenant_id='default' WHERE tenant_id IS NULL OR tenant_id='' ");
        }
    }
    private int count(String table) { Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); return c == null ? 0 : c; }
    private void seedMenus() {
        addMenu(1,0,"dashboard","首页 / 工作台","dashboard","/admin","Dashboard","dashboard:view",1);
        addMenu(2,0,"system","系统管理","system","/admin/system","Layout","system:view",10);
        addMenu(3,2,"tenants","租户管理","tenant","/admin/system/tenants","TenantManage","system:tenant:list",11);
        addMenu(4,2,"users","用户管理","user","/admin/system/users","UserManage","system:user:list",12);
        addMenu(5,2,"roles","角色管理","peoples","/admin/system/roles","RoleManage","system:role:list",13);
        addMenu(6,2,"menus","菜单管理","tree-table","/admin/system/menus","MenuManage","system:menu:list",14);
        addMenu(7,2,"dict","字典管理","dict","/admin/system/dict","DictManage","system:dict:list",15);
        addMenu(8,2,"configs","参数配置","config","/admin/system/configs","ConfigManage","system:config:list",16);
        addMenu(9,2,"logs","日志管理","log","/admin/system/logs","LogManage","system:log:list",17);
        addMenu(10,0,"selection","选品配置","shopping","/admin/selection","Layout","selection:view",20);
        addMenu(11,10,"sources","数据源配置","link","/admin/selection/sources","SourceConfig","selection:source:list",21);
        addMenu(12,10,"markets","市场配置","international","/admin/selection/markets","MarketConfig","selection:market:list",22);
        addMenu(13,10,"categories","品类配置","category","/admin/selection/categories","CategoryConfig","selection:category:list",23);
        addMenu(14,10,"schedules","采集频率配置","time","/admin/selection/schedules","ScheduleConfig","selection:schedule:list",24);
        addMenu(15,0,"reports","报表管理","chart","/admin/reports","Layout","reports:view",30);
        addMenu(16,15,"dailyReports","日报记录","date","/admin/reports/daily","DailyReports","reports:daily:list",31);
        addMenu(17,15,"productPool","商品池","goods","/admin/reports/products","ProductPool","reports:product:list",32);
    }

    private void addMenu(long id,long parent,String key,String title,String icon,String path,String component,String permission,int sort){ jdbc.update("INSERT INTO sys_menu(id,parent_id,menu_key,title,icon,path,component,permission,sort_order,status) VALUES(?,?,?,?,?,?,?,?,?,'enabled')", id,parent,key,title,icon,path,component,permission,sort); }

    public AdminProfile profile(String username) { SysUserInfo u = userInfo(username); return new AdminProfile(u.username, u.nickname, u.tenantId, List.of(u.roleKey), permissions(u.tenantId, u.roleKey)); }
    public boolean validateLogin(String username, String password) { ensureSeedData(); List<String> values = jdbc.query("SELECT password FROM sys_user WHERE username=? AND status='enabled'", (rs,n)->s(rs,1), username); return !values.isEmpty() && values.get(0).equals(password); }
    public String tenantOf(String username) { return userInfo(username).tenantId; }
    private SysUserInfo userInfo(String username) { ensureSeedData(); return jdbc.queryForObject("SELECT username,nickname,tenant_id,role_key FROM sys_user WHERE username=?", (rs,n)->new SysUserInfo(s(rs,"username"),s(rs,"nickname"),s(rs,"tenant_id"),s(rs,"role_key")), username); }
    private List<String> permissions(String tenantId, String roleKey) { List<String> menuKeys = jdbc.query("SELECT menu_keys FROM sys_role WHERE tenant_id=? AND role_key=?", (rs,n)->s(rs,1), tenantId, roleKey); if (menuKeys.isEmpty() || "*".equals(menuKeys.get(0))) return List.of("*:*:*"); return Arrays.stream(menuKeys.get(0).split(",")).map(String::trim).filter(s->!s.isBlank()).collect(Collectors.toList()); }
    private List<String> menuKeys(String tenantId, String roleKey) { List<String> values = jdbc.query("SELECT menu_keys FROM sys_role WHERE tenant_id=? AND role_key=?", (rs,n)->s(rs,1), tenantId, roleKey); return values.isEmpty() ? List.of() : Arrays.stream(values.get(0).split(",")).map(String::trim).filter(value -> !value.isBlank()).toList(); }
    private record SysUserInfo(String username,String nickname,String tenantId,String roleKey) {}

    public List<SysTenant> tenants(){ ensureSeedData(); return jdbc.query("SELECT * FROM sys_tenant ORDER BY id", (rs,n)->new SysTenant(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"tenant_name"),s(rs,"contact_user"),s(rs,"contact_phone"),s(rs,"package_name"),s(rs,"status"),s(rs,"remark"))); }
    public SysTenant saveTenant(SysTenant t){ ensureSeedData(); if(t.id()>0) jdbc.update("UPDATE sys_tenant SET tenant_id=?,tenant_name=?,contact_user=?,contact_phone=?,package_name=?,status=?,remark=? WHERE id=?",t.tenantId(),t.tenantName(),t.contactUser(),t.contactPhone(),t.packageName(),t.status(),t.remark(),t.id()); else jdbc.update("INSERT INTO sys_tenant(tenant_id,tenant_name,contact_user,contact_phone,package_name,status,remark) VALUES(?,?,?,?,?,?,?)",t.tenantId(),t.tenantName(),t.contactUser(),t.contactPhone(),t.packageName(),t.status(),t.remark()); settingsRepository.ensureDefault(t.tenantId()); return tenants().stream().filter(x->x.tenantId().equals(t.tenantId())).findFirst().orElse(tenants().get(0)); }
    public void deleteTenant(long id){ jdbc.update("DELETE FROM sys_tenant WHERE id=? AND tenant_id <> 'default'", id); }

    public List<AdminUser> users(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_user ORDER BY id":"SELECT * FROM sys_user WHERE tenant_id='"+tenantId+"' ORDER BY id"; return jdbc.query(sql, (rs,n)->new AdminUser(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"username"),"",s(rs,"nickname"),s(rs,"role_key"),s(rs,"status"),s(rs,"email"),s(rs,"phone"))); }
    public AdminUser saveUser(AdminUser u){ ensureSeedData(); if(u.id()>0) { if(u.password()==null||u.password().isBlank()) jdbc.update("UPDATE sys_user SET tenant_id=?,username=?,nickname=?,role_key=?,status=?,email=?,phone=? WHERE id=?", def(u.tenantId()),u.username(),u.nickname(),u.roleKey(),u.status(),u.email(),u.phone(),u.id()); else jdbc.update("UPDATE sys_user SET tenant_id=?,username=?,password=?,nickname=?,role_key=?,status=?,email=?,phone=? WHERE id=?", def(u.tenantId()),u.username(),u.password(),u.nickname(),u.roleKey(),u.status(),u.email(),u.phone(),u.id()); } else jdbc.update("INSERT INTO sys_user(tenant_id,username,password,nickname,role_key,status,email,phone) VALUES(?,?,?,?,?,?,?,?)", def(u.tenantId()),u.username(), blank(u.password(),"admin"), u.nickname(),u.roleKey(),u.status(),u.email(),u.phone()); return users("*").stream().filter(x->x.username().equals(u.username())).findFirst().orElse(users("*").get(0)); }
    public void deleteUser(long id){ jdbc.update("DELETE FROM sys_user WHERE id=? AND username <> 'admin'", id); }

    public List<AdminRole> roles(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_role ORDER BY id":"SELECT * FROM sys_role WHERE tenant_id='"+tenantId+"' ORDER BY id"; return jdbc.query(sql, (rs,n)->new AdminRole(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"role_key"),s(rs,"role_name"),s(rs,"status"),split(s(rs,"menu_keys")),s(rs,"remark"))); }
    public AdminRole saveRole(AdminRole r){ ensureSeedData(); if(r.id()>0) jdbc.update("UPDATE sys_role SET tenant_id=?,role_key=?,role_name=?,status=?,menu_keys=?,remark=? WHERE id=?",def(r.tenantId()),r.roleKey(),r.roleName(),r.status(),join(r.menuKeys()),r.remark(),r.id()); else jdbc.update("INSERT INTO sys_role(tenant_id,role_key,role_name,status,menu_keys,remark) VALUES(?,?,?,?,?,?)",def(r.tenantId()),r.roleKey(),r.roleName(),r.status(),join(r.menuKeys()),r.remark()); return roles("*").stream().filter(x->x.roleKey().equals(r.roleKey()) && x.tenantId().equals(def(r.tenantId()))).findFirst().orElse(roles("*").get(0)); }
    public void deleteRole(long id){ jdbc.update("DELETE FROM sys_role WHERE id=? AND role_key <> 'admin'", id); }

    public List<AdminMenu> menus(){ ensureSeedData(); List<AdminMenu> flat=jdbc.query("SELECT * FROM sys_menu ORDER BY sort_order,id", (rs,n)->new AdminMenu(rs.getLong("id"),rs.getLong("parent_id"),s(rs,"menu_key"),s(rs,"title"),s(rs,"icon"),s(rs,"path"),s(rs,"component"),s(rs,"permission"),rs.getInt("sort_order"),s(rs,"status"),new ArrayList<>())); return tree(flat,0); }
    public List<AdminMenu> menusFor(String username){ SysUserInfo user=userInfo(username); List<String> allowed=menuKeys(user.tenantId,user.roleKey); if(allowed.contains("*")) return menus(); List<AdminMenu> flat=flatMenus(); java.util.Set<Long> ids=new java.util.HashSet<>(); for(AdminMenu menu:flat) if(allowed.contains(menu.menuKey())) { ids.add(menu.id()); long parent=menu.parentId(); while(parent!=0){ long id=parent; ids.add(id); parent=flat.stream().filter(item->item.id()==id).findFirst().map(AdminMenu::parentId).orElse(0L); } } return tree(flat.stream().filter(menu->ids.contains(menu.id())).toList(),0); }
    public List<AdminMenu> flatMenus(){ ensureSeedData(); return jdbc.query("SELECT * FROM sys_menu ORDER BY sort_order,id", (rs,n)->new AdminMenu(rs.getLong("id"),rs.getLong("parent_id"),s(rs,"menu_key"),s(rs,"title"),s(rs,"icon"),s(rs,"path"),s(rs,"component"),s(rs,"permission"),rs.getInt("sort_order"),s(rs,"status"),List.of())); }
    public AdminMenu saveMenu(AdminMenu m){ ensureSeedData(); if(m.id()>0) jdbc.update("UPDATE sys_menu SET parent_id=?,menu_key=?,title=?,icon=?,path=?,component=?,permission=?,sort_order=?,status=? WHERE id=?",m.parentId(),m.menuKey(),m.title(),m.icon(),m.path(),m.component(),m.permission(),m.sortOrder(),m.status(),m.id()); else jdbc.update("INSERT INTO sys_menu(parent_id,menu_key,title,icon,path,component,permission,sort_order,status) VALUES(?,?,?,?,?,?,?,?,?)",m.parentId(),m.menuKey(),m.title(),m.icon(),m.path(),m.component(),m.permission(),m.sortOrder(),m.status()); return flatMenus().stream().filter(x->x.menuKey().equals(m.menuKey())).findFirst().orElse(flatMenus().get(0)); }
    public void deleteMenu(long id){ jdbc.update("DELETE FROM sys_menu WHERE id=?", id); }
    private List<AdminMenu> tree(List<AdminMenu> flat,long parent){ List<AdminMenu> out=new ArrayList<>(); for(AdminMenu m:flat) if(m.parentId()==parent) out.add(new AdminMenu(m.id(),m.parentId(),m.menuKey(),m.title(),m.icon(),m.path(),m.component(),m.permission(),m.sortOrder(),m.status(),tree(flat,m.id()))); return out; }

    public List<SysDictType> dictTypes(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_dict_type ORDER BY id":"SELECT * FROM sys_dict_type WHERE tenant_id='"+tenantId+"' ORDER BY id"; return jdbc.query(sql,(rs,n)->new SysDictType(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"dict_name"),s(rs,"dict_type"),s(rs,"status"),s(rs,"remark"))); }
    public SysDictType saveDictType(SysDictType d){ if(d.id()>0) jdbc.update("UPDATE sys_dict_type SET tenant_id=?,dict_name=?,dict_type=?,status=?,remark=? WHERE id=?",def(d.tenantId()),d.dictName(),d.dictType(),d.status(),d.remark(),d.id()); else jdbc.update("INSERT INTO sys_dict_type(tenant_id,dict_name,dict_type,status,remark) VALUES(?,?,?,?,?)",def(d.tenantId()),d.dictName(),d.dictType(),d.status(),d.remark()); return dictTypes("*").stream().filter(x->x.dictType().equals(d.dictType())).findFirst().orElse(dictTypes("*").get(0)); }
    public void deleteDictType(long id){ jdbc.update("DELETE FROM sys_dict_type WHERE id=?", id); }
    public List<SysDictData> dictData(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_dict_data ORDER BY dict_type,sort_order,id":"SELECT * FROM sys_dict_data WHERE tenant_id='"+tenantId+"' ORDER BY dict_type,sort_order,id"; return jdbc.query(sql,(rs,n)->new SysDictData(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"dict_type"),s(rs,"dict_label"),s(rs,"dict_value"),rs.getInt("sort_order"),s(rs,"status"),s(rs,"remark"))); }
    public SysDictData saveDictData(SysDictData d){ if(d.id()>0) jdbc.update("UPDATE sys_dict_data SET tenant_id=?,dict_type=?,dict_label=?,dict_value=?,sort_order=?,status=?,remark=? WHERE id=?",def(d.tenantId()),d.dictType(),d.dictLabel(),d.dictValue(),d.sortOrder(),d.status(),d.remark(),d.id()); else jdbc.update("INSERT INTO sys_dict_data(tenant_id,dict_type,dict_label,dict_value,sort_order,status,remark) VALUES(?,?,?,?,?,?,?)",def(d.tenantId()),d.dictType(),d.dictLabel(),d.dictValue(),d.sortOrder(),d.status(),d.remark()); return dictData("*").stream().filter(x->x.dictType().equals(d.dictType()) && x.dictValue().equals(d.dictValue())).findFirst().orElse(dictData("*").get(0)); }
    public void deleteDictData(long id){ jdbc.update("DELETE FROM sys_dict_data WHERE id=?", id); }

    public List<SysConfig> configs(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_config ORDER BY id":"SELECT * FROM sys_config WHERE tenant_id='"+tenantId+"' ORDER BY id"; return jdbc.query(sql,(rs,n)->new SysConfig(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"config_name"),s(rs,"config_key"),s(rs,"config_value"),rs.getBoolean("system_builtin"),s(rs,"remark"))); }
    public SysConfig saveConfig(SysConfig c){ if(c.id()>0) jdbc.update("UPDATE sys_config SET tenant_id=?,config_name=?,config_key=?,config_value=?,system_builtin=?,remark=? WHERE id=?",def(c.tenantId()),c.configName(),c.configKey(),c.configValue(),c.systemBuiltin(),c.remark(),c.id()); else jdbc.update("INSERT INTO sys_config(tenant_id,config_name,config_key,config_value,system_builtin,remark) VALUES(?,?,?,?,?,?)",def(c.tenantId()),c.configName(),c.configKey(),c.configValue(),c.systemBuiltin(),c.remark()); return configs("*").stream().filter(x->x.configKey().equals(c.configKey())).findFirst().orElse(configs("*").get(0)); }
    public void deleteConfig(long id){ jdbc.update("DELETE FROM sys_config WHERE id=?", id); }

    public List<MarketConfig> markets(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM market_configs ORDER BY id":"SELECT * FROM market_configs WHERE tenant_id='"+tenantId+"' ORDER BY id"; return jdbc.query(sql,(rs,n)->new MarketConfig(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"market_key"),s(rs,"market_name"),s(rs,"region"),rs.getBoolean("enabled"),s(rs,"note"))); }
    public MarketConfig saveMarket(MarketConfig m){ ensureSeedData(); if(m.id()>0) jdbc.update("UPDATE market_configs SET tenant_id=?,market_key=?,market_name=?,region=?,enabled=?,note=? WHERE id=?",def(m.tenantId()),m.marketKey(),m.marketName(),m.region(),m.enabled(),m.note(),m.id()); else jdbc.update("INSERT INTO market_configs(tenant_id,market_key,market_name,region,enabled,note) VALUES(?,?,?,?,?,?)",def(m.tenantId()),m.marketKey(),m.marketName(),m.region(),m.enabled(),m.note()); return markets("*").stream().filter(x->x.marketKey().equals(m.marketKey())).findFirst().orElse(markets("*").get(0)); }
    public List<CategoryConfig> categories(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM category_configs ORDER BY id":"SELECT * FROM category_configs WHERE tenant_id='"+tenantId+"' ORDER BY id"; return jdbc.query(sql,(rs,n)->new CategoryConfig(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"category_name"),s(rs,"market_key"),rs.getBoolean("enabled"),s(rs,"keywords"),s(rs,"note"))); }
    public CategoryConfig saveCategory(CategoryConfig c){ ensureSeedData(); if(c.id()>0) jdbc.update("UPDATE category_configs SET tenant_id=?,category_name=?,market_key=?,enabled=?,keywords=?,note=? WHERE id=?",def(c.tenantId()),c.categoryName(),c.marketKey(),c.enabled(),c.keywords(),c.note(),c.id()); else jdbc.update("INSERT INTO category_configs(tenant_id,category_name,market_key,enabled,keywords,note) VALUES(?,?,?,?,?,?)",def(c.tenantId()),c.categoryName(),c.marketKey(),c.enabled(),c.keywords(),c.note()); return categories("*").stream().filter(x->x.categoryName().equals(c.categoryName())).findFirst().orElse(categories("*").get(0)); }
    public ScheduleConfig schedule(String tenantId){ AdminSettings s=settingsRepository.get(def(tenantId)); return new ScheduleConfig(s.frequencyCron(),s.maxProducts(),s.jpyCnyRate(),s.defaultShippingCny(),s.smartMode()); }

    public void logOper(String tenantId,String username,String module,String action,String method,String status,String message){ jdbc.update("INSERT INTO sys_oper_log(tenant_id,username,module,action,method,status,message) VALUES(?,?,?,?,?,?,?)",def(tenantId),username,module,action,method,status,message); }
    public void logLogin(String tenantId,String username,String ipaddr,String status,String message){ jdbc.update("INSERT INTO sys_login_log(tenant_id,username,ipaddr,status,message) VALUES(?,?,?,?,?)",def(tenantId),username,ipaddr,status,message); }
    public List<SysOperLog> operLogs(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_oper_log ORDER BY id DESC LIMIT 200":"SELECT * FROM sys_oper_log WHERE tenant_id='"+tenantId+"' ORDER BY id DESC LIMIT 200"; return jdbc.query(sql,(rs,n)->new SysOperLog(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"username"),s(rs,"module"),s(rs,"action"),s(rs,"method"),s(rs,"status"),s(rs,"message"),instant(rs.getTimestamp("created_at")))); }
    public List<SysLoginLog> loginLogs(String tenantId){ ensureSeedData(); String sql="*".equals(tenantId)?"SELECT * FROM sys_login_log ORDER BY id DESC LIMIT 200":"SELECT * FROM sys_login_log WHERE tenant_id='"+tenantId+"' ORDER BY id DESC LIMIT 200"; return jdbc.query(sql,(rs,n)->new SysLoginLog(rs.getLong("id"),s(rs,"tenant_id"),s(rs,"username"),s(rs,"ipaddr"),s(rs,"status"),s(rs,"message"),instant(rs.getTimestamp("created_at")))); }

    private String def(String v){ return v==null || v.isBlank()?DEFAULT_TENANT:v; } private String blank(String v,String d){ return v==null || v.isBlank()?d:v; } private Instant instant(Timestamp t){ return t==null?Instant.EPOCH:t.toInstant(); }
    private List<String> split(String s){ return Arrays.stream((s==null?"":s).split(",")).map(String::trim).filter(x->!x.isBlank()).collect(Collectors.toList()); }
    private String join(List<String> v){ return v==null?"":v.stream().map(String::trim).filter(x->!x.isBlank()).collect(Collectors.joining(",")); }
    private String s(ResultSet rs, String column) throws SQLException { return rs.getString(column); }
    private String s(ResultSet rs, int index) throws SQLException { return rs.getString(index); }
}

package com.example.crossborder.service;

import com.example.crossborder.model.AdminMenu;
import com.example.crossborder.model.AdminRole;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.AdminUser;
import com.example.crossborder.model.CategoryConfig;
import com.example.crossborder.model.MarketConfig;
import com.example.crossborder.model.SysConfig;
import com.example.crossborder.model.SysDictData;
import com.example.crossborder.model.SysDictType;
import com.example.crossborder.model.SysTenant;
import com.example.crossborder.model.SupplierSiteConfig;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Service
public class AdminInputValidator {
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z][A-Za-z0-9:_-]{1,63}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]{2,63}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public void validateSettings(AdminSettings settings) {
        require(settings != null, "系统配置不能为空");
        requireText(settings.frequencyCron(), "采集 Cron 不能为空");
        try {
            CronExpression.parse(settings.frequencyCron().trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiValidationException("采集 Cron 格式不正确，应为 Spring 六段表达式");
        }
        requireRange(settings.maxProducts(), 1, 500, "每日报表商品数必须在 1 到 500 之间");
        requireRange(settings.maxCategories(), 1, 20, "采集大分类数必须在 1 到 20 之间");
        requireRange(settings.productsPerCategory(), 1, 30, "每个分类商品数必须在 1 到 30 之间");
        require(List.of("sales_volume", "sales_amount").contains(settings.rankingMetric()), "排行维度只能为销量指数或销售额指数");
        requireDecimal(settings.jpyCnyRate(), BigDecimal.ZERO, new BigDecimal("10"), false, "日元汇率必须大于 0 且不超过 10");
        requireDecimal(settings.defaultShippingCny(), BigDecimal.ZERO, new BigDecimal("100000"), true, "默认物流成本不合法");
        requireList(settings.categories(), "至少配置一个品类");
        requireList(settings.regions(), "至少配置一个区域");
        require(List.of("demo", "external", "mixed").contains(settings.sourceMode()), "数据模式只能为 demo、external 或 mixed");
        validateSupplierSites(settings.supplierSites());
    }

    public void validateUser(AdminUser user, boolean creating) {
        require(user != null, "用户不能为空");
        validateTenantId(user.tenantId());
        require(USERNAME.matcher(value(user.username())).matches(), "用户名应为 3-64 位字母、数字、点、下划线或短横线");
        requireText(user.nickname(), "昵称不能为空");
        validateIdentifier(user.roleKey(), "角色标识");
        validateStatus(user.status());
        if (creating) {
            requireText(user.password(), "新建用户必须设置初始密码");
        }
        if (user.password() != null && !user.password().isBlank()) {
            requireRange(user.password().length(), 8, 72, "密码长度必须在 8 到 72 个字符之间");
        }
        if (user.email() != null && !user.email().isBlank()) {
            require(EMAIL.matcher(user.email().trim()).matches(), "邮箱格式不正确");
        }
        requireLength(user.phone(), 64, "手机号长度不能超过 64");
    }

    public void validateLogin(String username, String password) {
        require(USERNAME.matcher(value(username)).matches(), "用户名格式不正确");
        requireText(password, "密码不能为空");
        requireLength(password, 72, "密码长度不能超过 72 个字符");
    }

    public void validateRole(AdminRole role) {
        require(role != null, "角色不能为空");
        validateTenantId(role.tenantId());
        validateIdentifier(role.roleKey(), "角色标识");
        requireText(role.roleName(), "角色名称不能为空");
        validateStatus(role.status());
        for (String menuKey : safe(role.menuKeys())) {
            validateIdentifier(menuKey, "菜单标识");
        }
    }

    public void validateMenu(AdminMenu menu, boolean updating) {
        require(menu != null, "菜单不能为空");
        require(menu.parentId() >= 0, "父级菜单不合法");
        require(!updating || menu.parentId() != menu.id(), "菜单不能将自身设为父级");
        validateIdentifier(menu.menuKey(), "菜单标识");
        requireText(menu.title(), "菜单名称不能为空");
        if (menu.path() != null && !menu.path().isBlank()) {
            require(menu.path().startsWith("/"), "菜单路由必须以 / 开头");
        }
        requireRange(menu.sortOrder(), 0, 100000, "菜单排序不合法");
        validateStatus(menu.status());
    }

    public void validateDictType(SysDictType type) {
        require(type != null, "字典类型不能为空");
        validateTenantId(type.tenantId());
        requireText(type.dictName(), "字典名称不能为空");
        validateIdentifier(type.dictType(), "字典类型");
        validateStatus(type.status());
    }

    public void validateDictData(SysDictData data) {
        require(data != null, "字典数据不能为空");
        validateTenantId(data.tenantId());
        validateIdentifier(data.dictType(), "字典类型");
        requireText(data.dictLabel(), "字典标签不能为空");
        requireText(data.dictValue(), "字典值不能为空");
        requireRange(data.sortOrder(), 0, 100000, "字典排序不合法");
        validateStatus(data.status());
    }

    public void validateConfig(SysConfig config) {
        require(config != null, "参数配置不能为空");
        validateTenantId(config.tenantId());
        requireText(config.configName(), "参数名称不能为空");
        validateIdentifier(config.configKey(), "参数键");
        requireLength(config.configValue(), 512, "参数值长度不能超过 512");
    }

    public void validateMarket(MarketConfig market) {
        require(market != null, "市场配置不能为空");
        validateTenantId(market.tenantId());
        validateIdentifier(market.marketKey(), "市场标识");
        requireText(market.marketName(), "市场名称不能为空");
        requireText(market.region(), "市场区域不能为空");
    }

    public void validateCategory(CategoryConfig category) {
        require(category != null, "品类配置不能为空");
        validateTenantId(category.tenantId());
        requireText(category.categoryName(), "品类名称不能为空");
        validateIdentifier(category.marketKey(), "市场标识");
    }

    public void validateTenant(SysTenant tenant) {
        require(tenant != null, "租户不能为空");
        validateTenantId(tenant.tenantId());
        requireText(tenant.tenantName(), "租户名称不能为空");
        validateStatus(tenant.status());
    }

    public void validateTenantFilter(String tenantId) {
        if ("*".equals(tenantId)) {
            return;
        }
        validateTenantId(tenantId);
    }

    public void validateTenantId(String tenantId) {
        validateIdentifier(tenantId, "租户标识");
    }

    private void validateIdentifier(String value, String label) {
        require(IDENTIFIER.matcher(value(value)).matches(), label + "应为 2-64 位字母、数字、冒号、下划线或短横线");
    }

    private void validateStatus(String status) {
        require("enabled".equals(status) || "disabled".equals(status), "状态只能为 enabled 或 disabled");
    }

    private void validateSupplierSites(List<SupplierSiteConfig> sites) {
        require(sites != null && !sites.isEmpty(), "至少配置一个国内采购搜索站点");
        requireRange(sites.size(), 1, 20, "国内采购搜索站点不能超过 20 个");
        for (SupplierSiteConfig site : sites) {
            require(site != null, "采购站点配置不能为空");
            requireText(site.name(), "采购站点名称不能为空");
            requireLength(site.name(), 64, "采购站点名称不能超过 64 个字符");
            requireText(site.urlTemplate(), "采购站点链接模板不能为空");
            require(site.urlTemplate().contains("{keyword}"), "采购站点链接模板必须包含 {keyword}");
            try {
                URI uri = URI.create(site.urlTemplate().replace("{keyword}", "sample"));
                require(List.of("http", "https").contains(uri.getScheme()) && uri.getHost() != null, "采购站点链接必须是完整的 HTTP/HTTPS 地址");
            } catch (IllegalArgumentException exception) {
                throw new ApiValidationException("采购站点链接模板格式不正确");
            }
        }
    }

    private void requireList(List<String> values, String message) {
        require(values != null && values.stream().anyMatch(value -> value != null && !value.isBlank()), message);
    }

    private void requireDecimal(BigDecimal value, BigDecimal min, BigDecimal max, boolean inclusiveMin, String message) {
        require(value != null, message);
        int lower = value.compareTo(min);
        require((inclusiveMin ? lower >= 0 : lower > 0) && value.compareTo(max) <= 0, message);
    }

    private void requireRange(int value, int min, int max, String message) {
        require(value >= min && value <= max, message);
    }

    private void requireLength(String text, int max, String message) {
        require(text == null || text.length() <= max, message);
    }

    private void requireText(String text, String message) {
        require(text != null && !text.isBlank(), message);
    }

    private void require(boolean expression, String message) {
        if (!expression) {
            throw new ApiValidationException(message);
        }
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }
}

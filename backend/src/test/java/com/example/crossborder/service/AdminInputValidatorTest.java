package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.model.AdminUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminInputValidatorTest {
    private final AdminInputValidator validator = new AdminInputValidator();

    @Test
    void rejectsInvalidCronBeforeItCanBreakTheScheduler() {
        AdminSettings invalid = new AdminSettings(
            List.of("Amazon/Rainforest"), List.of("1688"), List.of("玩具"), List.of("日本"),
            "external", "not-a-cron", 20, new BigDecimal("0.048"), true, new BigDecimal("18"), true
        );

        assertThrows(ApiValidationException.class, () -> validator.validateSettings(invalid));
    }

    @Test
    void validatesAUsableSettingsAndNewUserPassword() {
        AdminSettings valid = new AdminSettings(
            List.of("Amazon/Rainforest"), List.of("1688"), List.of("玩具"), List.of("日本"),
            "external", "0 30 8 * * *", 20, new BigDecimal("0.048"), true, new BigDecimal("18"), true
        );
        AdminUser user = new AdminUser(0, "default", "operator_1", "strong-password", "运营", "operator", "enabled", "operator@example.com", "");

        assertDoesNotThrow(() -> validator.validateSettings(valid));
        assertDoesNotThrow(() -> validator.validateUser(user, true));
        assertThrows(ApiValidationException.class, () -> validator.validateUser(new AdminUser(0, "default", "operator_1", "", "运营", "operator", "enabled", "", ""), true));
    }
}

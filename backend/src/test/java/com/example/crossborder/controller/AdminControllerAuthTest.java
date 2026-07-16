package com.example.crossborder.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.crossborder.repository.AdminDataRepository;
import com.example.crossborder.service.AdminAuthService;
import com.example.crossborder.service.AdminInputValidator;
import com.example.crossborder.service.AdminSettingsService;
import com.example.crossborder.service.JwtTokenService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AdminControllerAuthTest {
    @Test
    void logout_shouldAuditCurrentUserAndClientAddress() {
        AdminAuthService auth = mock(AdminAuthService.class);
        AdminSettingsService settings = mock(AdminSettingsService.class);
        AdminDataRepository data = mock(AdminDataRepository.class);
        AdminInputValidator validator = mock(AdminInputValidator.class);
        AdminController controller = new AdminController(auth, settings, data, validator);
        String authorization = "Bearer valid-token";
        when(auth.authorized(authorization)).thenReturn(true);
        when(auth.claims(authorization)).thenReturn(Optional.of(new JwtTokenService.Claims("operator1", "operator")));
        when(data.actor("operator1")).thenReturn(Optional.of(new AdminDataRepository.AdminActor("operator1", "default", "operator")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.8");

        Map<String, Object> response = controller.logout(authorization, request);

        assertEquals(true, response.get("loggedOut"));
        verify(data).logOper("default", "operator1", "登录认证", "注销", "/logout", "success", "用户主动退出后台");
        verify(data).logLogin("default", "operator1", "127.0.0.8", "success", "退出登录");
    }
}

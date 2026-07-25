package com.example.crossborder.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.crossborder.repository.AdminDataRepository;
import com.example.crossborder.service.AdminAuthService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ActionAuthControllerTest {
    @Test
    void correctPasswordIssuesShortLivedBackendToken() {
        AdminDataRepository data = mock(AdminDataRepository.class);
        AdminAuthService auth = mock(AdminAuthService.class);
        when(data.validateLogin("admin", "correct-password")).thenReturn(true);
        when(auth.issueActionToken()).thenReturn("signed-token");
        ActionAuthController controller = new ActionAuthController(data, auth);

        Map<String, Object> result = controller.verify(
            new ActionAuthController.PasswordRequest("correct-password")
        );

        assertEquals("signed-token", result.get("token"));
        assertEquals(1800, result.get("expiresIn"));
    }

    @Test
    void wrongPasswordIsRejected() {
        AdminDataRepository data = mock(AdminDataRepository.class);
        ActionAuthController controller = new ActionAuthController(
            data,
            mock(AdminAuthService.class)
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.verify(new ActionAuthController.PasswordRequest("wrong-password"))
        );

        assertEquals(401, exception.getStatusCode().value());
    }
}

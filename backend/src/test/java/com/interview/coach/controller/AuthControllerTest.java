package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.AuthLoginRequest;
import com.interview.coach.service.AuthService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.AuthResponseVO;
import com.interview.coach.vo.AuthUserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginReturnsAuthResponse() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("tester");
        request.setPassword("password123");
        AuthResponseVO expected = new AuthResponseVO();
        AuthUserVO user = new AuthUserVO();
        user.setId(3L);
        user.setUsername("tester");
        expected.setUser(user);
        expected.setToken("token");
        when(authService.login(request)).thenReturn(expected);

        ApiResponse<AuthResponseVO> response = controller.login(request);

        assertThat(response.getData().getUser().getId()).isEqualTo(3L);
        assertThat(response.getData().getToken()).isEqualTo("token");
    }

    @Test
    void meUsesCurrentUser() {
        AuthUserVO expected = new AuthUserVO();
        expected.setId(5L);
        expected.setUsername("tester5");
        when(currentUserContext.requireUserId()).thenReturn(5L);
        when(authService.me(5L)).thenReturn(expected);

        ApiResponse<AuthUserVO> response = controller.me();

        assertThat(response.getData().getUsername()).isEqualTo("tester5");
    }
}

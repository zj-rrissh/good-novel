package com.ainovel.user.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.common.api.Result;
import com.ainovel.user.dto.LoginRequest;
import com.ainovel.user.dto.RefreshTokenRequest;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.service.AuthService;
import com.ainovel.user.vo.AccessTokenVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AccessTokenVO> register(@Valid @RequestBody RegisterRequest request,
                                          @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                          String idempotencyKey) {
        return Result.success(authService.register(request, idempotencyKey));
    }

    @PostMapping("/login")
    public Result<AccessTokenVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<AccessTokenVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request));
    }
}

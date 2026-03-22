package com.ainovel.user.service;

import com.ainovel.user.dto.LoginRequest;
import com.ainovel.user.dto.RefreshTokenRequest;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.vo.AccessTokenVO;

public interface AuthService {

    AccessTokenVO register(RegisterRequest request, String idempotencyKey);

    AccessTokenVO login(LoginRequest request);

    AccessTokenVO refresh(RefreshTokenRequest request);

    void logout();
}

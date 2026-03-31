package com.ainovel.user.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.user.dto.ChangePasswordRequest;
import com.ainovel.user.dto.LoginRequest;
import com.ainovel.user.dto.RefreshTokenRequest;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.vo.AccessTokenVO;
import com.ainovel.user.vo.LoginRecordVO;

public interface AuthService {

    AccessTokenVO register(RegisterRequest request, String idempotencyKey);

    AccessTokenVO login(LoginRequest request);

    AccessTokenVO refresh(RefreshTokenRequest request);

    void logout();

    void changePassword(ChangePasswordRequest request);

    PageResponse<LoginRecordVO> queryLoginRecords(int page, int size);
}

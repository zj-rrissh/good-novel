package com.ainovel.user.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.user.dto.ChangePasswordRequest;
import com.ainovel.user.dto.UpdateUserProfileRequest;
import com.ainovel.user.service.AuthService;
import com.ainovel.user.service.UserProfileService;
import com.ainovel.user.vo.LoginRecordVO;
import com.ainovel.user.vo.UserMeVO;
import com.ainovel.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthService authService;

    public UserProfileController(UserProfileService userProfileService, AuthService authService) {
        this.userProfileService = userProfileService;
        this.authService = authService;
    }

    @GetMapping
    public Result<UserMeVO> getMe() {
        return Result.success(userProfileService.currentUser());
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return Result.success(userProfileService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success();
    }

    @GetMapping("/login-records")
    public Result<PageResponse<LoginRecordVO>> loginRecords(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(authService.queryLoginRecords(page, size));
    }
}

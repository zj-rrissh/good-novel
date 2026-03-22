package com.ainovel.user.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.user.dto.UpdateUserProfileRequest;
import com.ainovel.user.service.UserProfileService;
import com.ainovel.user.vo.UserMeVO;
import com.ainovel.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public Result<UserMeVO> getMe() {
        return Result.success(userProfileService.currentUser());
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return Result.success(userProfileService.updateProfile(request));
    }
}

package com.ainovel.user.service;

import com.ainovel.user.dto.UpdateUserProfileRequest;
import com.ainovel.user.vo.UserMeVO;
import com.ainovel.user.vo.UserProfileVO;

public interface UserProfileService {

    UserMeVO currentUser();

    UserProfileVO updateProfile(UpdateUserProfileRequest request);
}

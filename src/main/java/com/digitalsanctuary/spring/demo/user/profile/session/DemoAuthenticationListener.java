package com.digitalsanctuary.spring.demo.user.profile.session;

import org.springframework.stereotype.Component;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfile;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileService;
import com.digitalsanctuary.spring.user.profile.session.BaseAuthenticationListener;

@Component
public class DemoAuthenticationListener extends BaseAuthenticationListener<DemoUserProfile> {

    public DemoAuthenticationListener(DemoSessionProfile sessionProfile, DemoUserProfileService profileService) {
        super(sessionProfile, profileService);
    }
}

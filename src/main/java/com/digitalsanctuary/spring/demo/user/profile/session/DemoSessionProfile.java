package com.digitalsanctuary.spring.demo.user.profile.session;

import org.springframework.stereotype.Component;
import com.digitalsanctuary.spring.demo.event.Event;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfile;
import com.digitalsanctuary.spring.user.profile.session.BaseSessionProfile;

@Component
public class DemoSessionProfile extends BaseSessionProfile<DemoUserProfile> {

    public boolean isRegisteredForEvent(Event event) {
        return getUserProfile() != null && getUserProfile().getEventRegistrations().stream().anyMatch(reg -> reg.getEvent().equals(event));
    }

    public String getFavoriteColor() {
        return getUserProfile() != null ? getUserProfile().getFavoriteColor() : null;
    }
}

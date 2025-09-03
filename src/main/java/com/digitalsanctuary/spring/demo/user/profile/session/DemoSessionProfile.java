package com.digitalsanctuary.spring.demo.user.profile.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.digitalsanctuary.spring.demo.event.Event;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfile;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileRepository;
import com.digitalsanctuary.spring.user.profile.session.BaseSessionProfile;

@Component
public class DemoSessionProfile extends BaseSessionProfile<DemoUserProfile> {

    @Autowired
    private DemoUserProfileRepository profileRepository;

    public boolean isRegisteredForEvent(Event event) {
        return getUserProfile() != null && getUserProfile().getEventRegistrations().stream().anyMatch(reg -> reg.getEvent().equals(event));
    }

    public String getFavoriteColor() {
        return getUserProfile() != null ? getUserProfile().getFavoriteColor() : null;
    }
    
    /**
     * Refreshes the user profile from the database to ensure we have the latest data
     * @return the refreshed user profile or null if no profile exists
     */
    public DemoUserProfile refreshProfile() {
        DemoUserProfile currentProfile = getUserProfile();
        if (currentProfile != null && currentProfile.getId() != null) {
            DemoUserProfile refreshedProfile = profileRepository.findById(currentProfile.getId()).orElse(null);
            if (refreshedProfile != null) {
                // Update the session with the refreshed profile
                setUserProfile(refreshedProfile);
                return refreshedProfile;
            }
        }
        return currentProfile;
    }
}

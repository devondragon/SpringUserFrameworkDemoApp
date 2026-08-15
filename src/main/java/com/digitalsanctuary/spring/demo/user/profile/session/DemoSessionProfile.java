package com.digitalsanctuary.spring.demo.user.profile.session;

import org.springframework.beans.factory.annotation.Autowired;
import com.digitalsanctuary.spring.demo.event.Event;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfile;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileRepository;
import com.digitalsanctuary.spring.user.profile.session.BaseSessionProfile;
import com.digitalsanctuary.spring.user.profile.session.SessionScopedProfile;

/**
 * Session-scoped profile for the demo user.
 *
 * Annotated with {@link SessionScopedProfile} rather than plain {@code @Component}. Spring's {@code @Scope} is
 * not inherited from {@link BaseSessionProfile}, so a plain {@code @Component} here would make this a singleton
 * shared by every HTTP session and leak one user's profile to all other users.
 */
@SessionScopedProfile
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

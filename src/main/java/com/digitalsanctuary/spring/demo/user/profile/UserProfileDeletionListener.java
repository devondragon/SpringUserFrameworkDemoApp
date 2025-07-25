package com.digitalsanctuary.spring.demo.user.profile;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.digitalsanctuary.spring.user.event.UserPreDeleteEvent;

/**
 * Listener for user profile deletion events. This class listens for UserPreDeleteEvent and deletes the associated DemoUserProfile. It is assumed that
 * the DemoUserProfile is mapped to the User entity with a one-to-one relationship.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileDeletionListener {
    private final DemoUserProfileRepository demoUserProfileRepository;
    // Inject other repositories if needed (e.g., EventRegistrationRepository)

    @EventListener
    @Transactional // Joins the transaction started by UserService.deleteUserAccount
    public void handleUserPreDelete(UserPreDeleteEvent event) {
        Long userId = event.getUser().getId();
        log.info("Received UserPreDeleteEvent for userId: {}. Deleting associated DemoUserProfile...", userId);

        // Option 1: Delete profile directly (if no further cascades needed from profile)
        // Since DemoUserProfile uses @MapsId, its ID is the same as the User's ID
        demoUserProfileRepository.findById(userId).ifPresent(profile -> {
            log.debug("Found DemoUserProfile for userId: {}. Deleting...", userId);
            // If DemoUserProfile itself has relationships needing cleanup (like EventRegistrations)
            // that aren't handled by CascadeType.REMOVE or orphanRemoval=true,
            // handle them here *before* deleting the profile.
            // Example: eventRegistrationRepository.deleteByUserProfile(profile);
            demoUserProfileRepository.delete(profile);
            log.debug("DemoUserProfile deleted for userId: {}", userId);
        });

        // Option 2: If DemoUserProfile has CascadeType.REMOVE/orphanRemoval=true
        // on its collections (like eventRegistrations), deleting the profile might be enough.
        // demoUserProfileRepository.deleteById(userId);

        log.info("Finished processing UserPreDeleteEvent for userId: {}", userId);
    }
}

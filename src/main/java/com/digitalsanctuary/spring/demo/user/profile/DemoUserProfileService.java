package com.digitalsanctuary.spring.demo.user.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digitalsanctuary.spring.demo.event.Event;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.profile.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A demo implementation of the {@link UserProfileService} interface for managing user profiles in a Spring Boot application.
 *
 * <p>
 * This class serves as a concrete example for developers looking to integrate user profile management into their own applications. It demonstrates
 * how to:
 * <ul>
 * <li>Retrieve or create user profiles associated with a {@link User} entity.</li>
 * <li>Update user profiles with new information.</li>
 * <li>Extend profile functionality with custom methods such as event registration.</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Leverages Spring's {@link Service} annotation for dependency injection and transactional boundaries.</li>
 * <li>Demonstrates the use of a type-safe implementation with generics ({@code DemoUserProfile}).</li>
 * <li>Provides robust error handling to ensure null-safe operations.</li>
 * </ul>
 *
 * <h2>How to Use This Class</h2> Developers can use this class as a template to create their own implementations of {@link UserProfileService}.
 * Follow these steps:
 * <ol>
 * <li>Create a custom user profile entity that extends {@code BaseUserProfile}.</li>
 * <li>Create a repository interface for the custom profile entity, extending Spring Data's {@code JpaRepository}.</li>
 * <li>Implement the {@link UserProfileService} interface, similar to this demo, adapting it to the requirements of your application.</li>
 * <li>Define additional methods specific to your use case, such as handling domain-specific logic (e.g., event registration).</li>
 * </ol>
 *
 * <h3>Example Custom Implementation</h3>
 *
 * <pre>
 * {
 *     &#64;code
 *     &#64;Service
 *     &#64;Transactional
 *     &#64;RequiredArgsConstructor
 *     public class CustomUserProfileService implements UserProfileService<CustomUserProfile> {
 *
 *         private final CustomUserProfileRepository profileRepository;
 *         private final UserRepository userRepository;
 *
 *         &#64;Override
 *         public CustomUserProfile getOrCreateProfile(User user) {
 *             if (user == null) {
 *                 throw new IllegalArgumentException("User must not be null");
 *             }
 *             return profileRepository.findByUserId(user.getId()).orElseGet(() -> {
 *                 User managedUser = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
 *                 CustomUserProfile profile = new CustomUserProfile();
 *                 profile.setUser(managedUser);
 *                 return profileRepository.save(profile);
 *             });
 *         }
 *
 *         @Override
 *         public CustomUserProfile updateProfile(CustomUserProfile profile) {
 *             if (profile == null) {
 *                 throw new IllegalArgumentException("Profile must not be null");
 *             }
 *             return profileRepository.save(profile);
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>About This Implementation</h2>
 * <p>
 * The {@code DemoUserProfileService} class includes the following methods:
 * </p>
 *
 * <ul>
 * <li>{@link #getOrCreateProfile(User)}: Ensures that every {@link User} has an associated profile, creating a new one if none exists. Demonstrates
 * how to fetch and persist entities using Spring Data repositories.</li>
 * <li>{@link #updateProfile(DemoUserProfile)}: Saves updated profile data to the database, with null checks for safety.</li>
 * <li>{@link #registerForEvent(DemoUserProfile, Event)}: A custom extension method that links an event to a user profile, showcasing how to add
 * application-specific functionality.</li>
 * </ul>
 *
 * <h2>Extensibility</h2> This implementation can be easily extended to include more methods and logic based on your application's requirements.
 * Examples include:
 * <ul>
 * <li>Managing profile settings or preferences.</li>
 * <li>Linking profiles to additional domain entities (e.g., orders, subscriptions).</li>
 * <li>Adding caching for frequently accessed profile data.</li>
 * </ul>
 *
 * <h2>Annotations and Frameworks</h2>
 * <ul>
 * <li>{@link Service}: Marks this class as a Spring-managed service bean.</li>
 * <li>{@link Transactional}: Ensures all operations are executed within a database transaction.</li>
 * <li>{@link RequiredArgsConstructor}: Automatically generates a constructor for final fields, simplifying dependency injection.</li>
 * </ul>
 *
 * @see UserProfileService
 * @see DemoUserProfile
 * @see User
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DemoUserProfileService implements UserProfileService<DemoUserProfile> {

    private final DemoUserProfileRepository profileRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves an existing profile for the given user or creates a new one if none exists.
     *
     * @param user the user to get or create a profile for
     * @return the existing or newly created profile
     * @throws IllegalArgumentException if the user is null or not found
     */
    @Override
    public DemoUserProfile getOrCreateProfile(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return profileRepository.findByUserId(user.getId()).orElseGet(() -> createAndSaveProfile(user));
    }

    /**
     * Creates and saves a new profile for the given user.
     *
     * @param user the user for whom the profile is being created
     * @return the newly created and persisted profile
     */
    private DemoUserProfile createAndSaveProfile(User user) {
        User managedUser = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));

        DemoUserProfile profile = new DemoUserProfile();
        profile.setUser(managedUser);
        return profileRepository.save(profile);
    }

    /**
     * Updates an existing user profile with new information.
     *
     * @param profile the profile to update
     * @return the updated profile
     * @throws IllegalArgumentException if the profile is null
     */
    @Override
    public DemoUserProfile updateProfile(DemoUserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile must not be null");
        }
        return profileRepository.save(profile);
    }

    /**
     * Registers the given profile for a specific event.
     *
     * <p>
     * This method demonstrates how to extend profile functionality with application-specific logic.
     *
     * @param profile the profile to register for the event
     * @param event the event to register for
     * @return the updated profile with the event registration
     * @throws IllegalArgumentException if the profile or event is null
     */
    @Transactional // added to ensure the session remains active
    public DemoUserProfile registerForEvent(DemoUserProfile profile, Event event) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile must not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null");
        }

        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        profile.addEventRegistration(registration);
        return profileRepository.save(profile);
    }

    /**
     * Unregisters the given profile from a specific event.
     *
     * <p>
     * This method demonstrates how to extend profile functionality with application-specific logic.
     *
     * @param profile the profile to unregister from the event
     * @param event the event to unregister from
     * @return the updated profile without the event registration
     * @throws IllegalArgumentException if the profile or event is null
     */
    public DemoUserProfile unregisterFromEvent(DemoUserProfile profile, Event event) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile must not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null");
        }
        profile.removeEventRegistration(event);
        log.info("Unregistered profile {} from event {}", profile.getId(), event.getId());
        return profileRepository.save(profile);
    }
}

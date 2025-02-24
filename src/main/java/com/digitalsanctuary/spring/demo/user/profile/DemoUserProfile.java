package com.digitalsanctuary.spring.demo.user.profile;

import java.util.ArrayList;
import java.util.List;
import com.digitalsanctuary.spring.demo.event.Event;
import com.digitalsanctuary.spring.user.profile.BaseUserProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "demo_user_profile")
@EqualsAndHashCode(callSuper = true)
public class DemoUserProfile extends BaseUserProfile {


    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EventRegistration> eventRegistrations = new ArrayList<>();

    private String favoriteColor;
    private boolean receiveNewsletter;

    public void addEventRegistration(EventRegistration registration) {
        eventRegistrations.add(registration);
        registration.setUserProfile(this);
    }

    public void removeEventRegistration(EventRegistration registration) {
        eventRegistrations.remove(registration);
        registration.setUserProfile(null);
    }

    public EventRegistration removeEventRegistration(Event event) {
        EventRegistration registration =
                eventRegistrations.stream().filter(reg -> reg.getEvent().getId().equals(event.getId())).findFirst().orElse(null);
        if (registration != null) {
            eventRegistrations.remove(registration);
        }
        return registration;
    }

    public boolean isRegisteredForEvent(Event event) {
        return eventRegistrations.stream().anyMatch(reg -> reg.getEvent().equals(event));
    }

    public void removeEventRegistration(Long eventId) {
        eventRegistrations.removeIf(reg -> reg.getEvent().getId().equals(eventId));
    }

    public boolean isRegisteredForEvent(Long eventId) {
        return eventRegistrations.stream().anyMatch(reg -> reg.getEvent().getId().equals(eventId));
    }
}

package com.digitalsanctuary.spring.user.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.digitalsanctuary.spring.user.persistence.model.Privilege;
import com.digitalsanctuary.spring.user.persistence.model.Role;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.persistence.repository.UserRepository;
import com.digitalsanctuary.spring.user.service.DSUserDetails;
import com.digitalsanctuary.spring.user.service.LoginHelperService;

/**
 * Test configuration for API tests.
 * Provides proper Spring Security integration for @WithUserDetails.
 */
@TestConfiguration
public class ApiTestConfiguration {
    
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService(UserRepository userRepository, LoginHelperService loginHelperService) {
        return username -> {
            User user = userRepository.findByEmail(username);
            if (user != null) {
                return new DSUserDetails(user, getAuthorities(user.getRoles()));
            }
            throw new RuntimeException("User not found: " + username);
        };
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(final Collection<Role> roles) {
        final List<String> privileges = new ArrayList<>();
        final List<Privilege> collection = new ArrayList<>();
        for (final Role role : roles) {
            collection.addAll(role.getPrivileges());
        }
        for (final Privilege item : collection) {
            privileges.add(item.getName());
        }
        return getGrantedAuthorities(privileges);
    }

    private List<GrantedAuthority> getGrantedAuthorities(final List<String> privileges) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        for (final String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }
}
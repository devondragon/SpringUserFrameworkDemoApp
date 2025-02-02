package com.digitalsanctuary.spring.demo.user.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoUserProfileRepository extends JpaRepository<DemoUserProfile, Long> {
    Optional<DemoUserProfile> findByUserId(Long userId);

}

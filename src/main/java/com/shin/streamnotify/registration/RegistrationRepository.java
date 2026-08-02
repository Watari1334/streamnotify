package com.shin.streamnotify.registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, RegistrationId> {

    List<Registration> findByUser_UserId(Long userId);
    List<Registration> findByStreamer_StreamerId(Long streamerId);

    Optional<Registration> findByUser_UserIdAndStreamer_StreamerId(Long userId, Long streamerId);

    long countByUser_UserId(Long userId);
}
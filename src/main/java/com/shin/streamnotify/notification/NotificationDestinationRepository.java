package com.shin.streamnotify.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationDestinationRepository extends JpaRepository<NotificationDestination, Long> {

    Optional<NotificationDestination> findByUser_UserId(Long userId);
}
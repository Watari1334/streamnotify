package com.shin.streamnotify.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDestinationRepository extends JpaRepository<NotificationDestination, Long> {

    List<NotificationDestination> findByUser_UserId(Long userId);
}
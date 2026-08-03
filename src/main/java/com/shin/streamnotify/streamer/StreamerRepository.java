package com.shin.streamnotify.streamer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StreamerRepository extends JpaRepository<Streamer, Long> {

    Optional<Streamer> findByPlatformAndPlatformChannelId(String platform, String platformChannelId);

    List<Streamer> findByPlatform(String platform);
}
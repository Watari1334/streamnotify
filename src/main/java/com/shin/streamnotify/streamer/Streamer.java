package com.shin.streamnotify.streamer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "streamers")
@Getter
@Setter
@NoArgsConstructor
public class Streamer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "streamer_id")
    private Long streamerId;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "platform_channel_id", nullable = false)
    private String platformChannelId;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Streamer(String platform, String platformChannelId, String channelName) {
        this.platform = platform;
        this.platformChannelId = platformChannelId;
        this.channelName = channelName;
        this.createdAt = LocalDateTime.now();
    }
}
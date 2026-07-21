package com.shin.streamnotify.streamer;

import java.time.LocalDateTime;

public record StreamerResponse(
        Long streamerId,
        String platform,
        String platformChannelId,
        String channelName,
        LocalDateTime registeredAt
) {
}
package com.shin.streamnotify.streamer;

public record StreamerRegistrationRequest(
        String platform,
        String platformChannelId,
        String channelName,
        String channelLogin
) {
}
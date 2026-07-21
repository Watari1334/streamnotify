package com.shin.streamnotify.notification;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class DiscordNotificationService {

    private final RestClient restClient = RestClient.create();

    public void sendStreamOnlineNotification(String webhookUrl, String channelName) {
        Map<String, Object> requestBody = Map.of(
                "content", "🔴 **" + channelName + "** が配信を開始しました!"
        );

        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}

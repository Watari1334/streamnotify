package com.shin.streamnotify.twitch;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TwitchEventSubService {

    private final RestClient restClient = RestClient.create();
    private final TwitchAuthService twitchAuthService;

    @Value("${spring.security.oauth2.client.registration.twitch.client-id}")
    private String clientId;

    @Value("${twitch.eventsub.secret}")
    private String eventSubSecret;

    @Value("${twitch.eventsub.callback-url}")
    private String callbackUrl;

    public void subscribeToStreamOnline(String broadcasterUserId) {
        String appAccessToken = twitchAuthService.getAppAccessToken();

        Map<String, Object> requestBody = Map.of(
                "type", "stream.online",
                "version", "1",
                "condition", Map.of("broadcaster_user_id", broadcasterUserId),
                "transport", Map.of(
                        "method", "webhook",
                        "callback", callbackUrl,
                        "secret", eventSubSecret
                )
        );

        String response = restClient.post()
                .uri("https://api.twitch.tv/helix/eventsub/subscriptions")
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Client-Id", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        System.out.println("EventSub購読登録結果: " + response);
    }
    public String listSubscriptions() {
        String appAccessToken = twitchAuthService.getAppAccessToken();

        return restClient.get()
                .uri("https://api.twitch.tv/helix/eventsub/subscriptions")
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Client-Id", clientId)
                .retrieve()
                .body(String.class);
    }
}
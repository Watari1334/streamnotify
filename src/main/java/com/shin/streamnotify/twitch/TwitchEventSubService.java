package com.shin.streamnotify.twitch;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
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

    public String subscribeToStreamOnline(String broadcasterUserId) {
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

        SubscriptionResponse response = restClient.post()
                .uri("https://api.twitch.tv/helix/eventsub/subscriptions")
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Client-Id", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(SubscriptionResponse.class);

        System.out.println("EventSub購読登録結果: " + response);

        return response.data().get(0).id();
    }

    public void unsubscribe(String subscriptionId) {
        String appAccessToken = twitchAuthService.getAppAccessToken();

        restClient.delete()
                .uri("https://api.twitch.tv/helix/eventsub/subscriptions?id=" + subscriptionId)
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Client-Id", clientId)
                .retrieve()
                .toBodilessEntity();
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

    private record SubscriptionResponse(List<SubscriptionData> data) {
    }

    private record SubscriptionData(String id) {
    }

    @Cacheable(value = "channelSearch", key = "#query")
    public List<ChannelSearchResult> searchChannels(String query) {
        String appAccessToken = twitchAuthService.getAppAccessToken();

        SearchResponse response = restClient.get()
                .uri("https://api.twitch.tv/helix/search/channels?query=" + query)
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Client-Id", clientId)
                .retrieve()
                .body(SearchResponse.class);

        return response.data();
    }

    private record SearchResponse(List<ChannelSearchResult> data) {
    }

    public record ChannelSearchResult(
            String id,
            String display_name,
            boolean is_live
    ) {
    }
}
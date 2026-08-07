package com.shin.streamnotify.twitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;

/**
 * TwitchEventSub(配信通知)の購読管理と、チャンネル検索を扱うサービス。
 * 実際の配信開始検知・通知処理はTwitchWebhookControllerが担当する。
 */
@Slf4j
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

    /**
     * 指定した配信者のstream.onlineイベント(配信開始)を購読する。
     * TwitchEventSubのWebhook方式で登録し、購読成功時にIDが発行される。
     *
     * @param broadcasterUserId 購読対象のTwitchチャンネルID
     * @return 発行された購読ID(後で解除する際に使用する)
     */
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

        log.info("EventSub購読登録結果: {}", response);

        return response.data().get(0).id();
    }

    /**
     * 指定した購読IDのEventSub購読を解除する。
     *
     * @param subscriptionId 解除する購読ID
     */
    public void unsubscribe(String subscriptionId) {
        String appAccessToken = twitchAuthService.getAppAccessToken();

        restClient.delete()
                .uri("https://api.twitch.tv/helix/eventsub/subscriptions?id=" + subscriptionId)
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Client-Id", clientId)
                .retrieve()
                .toBodilessEntity();
    }

    private record SubscriptionResponse(List<SubscriptionData> data) {
    }

    private record SubscriptionData(String id) {
    }

    /**
     * Twitchのチャンネルをキーワードで検索する。
     * 結果はRedisに5分間キャッシュされる(twitchchannelSearch)。
     *
     * @param query 検索キーワード
     * @return 検索結果一覧
     */
    @Cacheable(value = "twitchchannelSearch", key = "#query")
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

    /**
     * Twitchチャンネル検索結果のDTO。
     *
     * @param id Twitchのブロードキャスター(チャンネル)ID
     * @param broadcaster_login チャンネルのログイン名(URLスラッグ)
     * @param display_name チャンネルの表示名
     * @param thumbnail_url サムネイル画像のURL
     */
    public record ChannelSearchResult(
            String id,
            String broadcaster_login,
            String display_name,
            String thumbnail_url
    ) {
    }
}
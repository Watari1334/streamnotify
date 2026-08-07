package com.shin.streamnotify.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * YouTubeのPubSubHubbubの購読管理、チャンネル検索、配信中判定を扱うサービス。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeEventSubService {

    private final RestClient restClient = RestClient.create();

    @Value("${youtube.eventsub.secret}")
    private String eventSubSecret;

    @Value("${youtube.eventsub.callback-url}")
    private String callbackUrl;

    @Value("${youtube.api.key}")
    private String apiKey;

    private static final String HUB_URL = "https://pubsubhubbub.appspot.com/subscribe";

    /**
     * 指定したチャンネルのPubSubHubbub購読を登録する。
     * hub.secretを指定することで、通知受信時にYouTubeWebhookController側で
     * 署名(X-Hub-Signature)を検証できるようにしている。
     *
     * @param channelId 購読対象のYouTubeチャンネルID
     */
    public void subscribe(String channelId) {
        String topicUrl = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + channelId;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("hub.mode", "subscribe");
        formData.add("hub.topic", topicUrl);
        formData.add("hub.callback", callbackUrl);
        formData.add("hub.verify", "sync");
        formData.add("hub.secret", eventSubSecret);
        formData.add("hub.lease_seconds", "432000");

        ResponseEntity<String> response = restClient.post()
                .uri(HUB_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(String.class);

        log.info("PubSubHubbub購読結果、ステータス: {}", response.getStatusCode());
    }

    /**
     * 指定したチャンネルのPubSubHubbub購読を解除する。
     *
     * @param channelId 購読解除対象のYouTubeチャンネルID
     */
    public void unsubscribe(String channelId) {
        String topicUrl = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + channelId;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("hub.mode", "unsubscribe");
        formData.add("hub.topic", topicUrl);
        formData.add("hub.callback", callbackUrl);

        restClient.post()
                .uri(HUB_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * YouTubeのチャンネルをキーワードで検索する。
     * 結果はRedisに24時間キャッシュされる(youtubeChannelSearch)。
     * YouTube Data APIのクォータ(search.listは1回100ユニット、1日10,000ユニット上限)が
     * 厳しいため、TTLを長めに設定している。
     *
     * @param query 検索キーワード
     * @return 検索結果一覧
     */
    @Cacheable(value = "youtubeChannelSearch", key = "#query")
    public List<ChannelSearchResult> searchChannels(String query) {
        SearchResponse response = restClient.get()
                .uri("https://www.googleapis.com/youtube/v3/search?part=snippet&type=channel&q=" + query + "&key=" + apiKey)
                .retrieve()
                .body(SearchResponse.class);

        return response.items().stream()
                .map(item -> new ChannelSearchResult(
                        item.id().channelId(),
                        item.snippet().channelTitle(),
                        item.snippet().thumbnails().thumbnailDefault().url()
                ))
                .toList();
    }

    private record SearchResponse(List<SearchItem> items) {
    }

    private record SearchItem(SearchItemId id, Snippet snippet) {
    }

    private record SearchItemId(String channelId) {
    }

    private record Snippet(String channelTitle, Thumbnails thumbnails) {
    }

    private record Thumbnails(@JsonProperty("default") Thumbnail thumbnailDefault) {
    }

    private record Thumbnail(String url) {
    }

    /**
     * YouTubeチャンネル検索結果のDTO。
     *
     * @param channelId YouTubeチャンネルID
     * @param channelTitle チャンネル名
     * @param thumbnailUrl サムネイル画像のURL
     */
    public record ChannelSearchResult(
            String channelId,
            String channelTitle,
            String thumbnailUrl
    ) {
    }

    /**
     * 指定した動画IDが、現在ライブ配信中かどうかを判定する。
     * 配信開始時刻(actualStartTime)が記録されていて、かつ
     * 終了時刻(actualEndTime)が記録されていない場合のみtrueを返す。
     * PubSubHubbubの通知だけでは配信中かどうか確定できないため、
     * この呼び出しでYouTube側の実際の状態を再確認する目的で使われる。
     *
     * @param videoId 判定対象のYouTube動画(配信)ID
     * @return 配信中であればtrue
     */
    public boolean isLive(String videoId) {
        VideoResponse response = restClient.get()
                .uri("https://www.googleapis.com/youtube/v3/videos?part=liveStreamingDetails&id=" + videoId + "&key=" + apiKey)
                .retrieve()
                .body(VideoResponse.class);

        if (response.items().isEmpty()) {
            return false;
        }

        LiveStreamingDetails details = response.items().get(0).liveStreamingDetails();

        return details != null
                && details.actualStartTime() != null
                && details.actualEndTime() == null;
    }

    private record VideoResponse(List<VideoItem> items) {
    }

    private record VideoItem(LiveStreamingDetails liveStreamingDetails) {
    }

    private record LiveStreamingDetails(String actualStartTime, String actualEndTime) {
    }
}
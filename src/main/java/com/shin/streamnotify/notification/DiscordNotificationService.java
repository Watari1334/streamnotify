package com.shin.streamnotify.notification;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Discord Webhookへ配信開始通知を送信するサービス。
 * TwitchとYouTubeで通知リンクの組み立て方が異なる
 * (Twitchはchannel_login、YouTubeはvideoIdを使う)ため、
 * プラットフォームごとに分岐している。
 */
@Service
public class DiscordNotificationService {

    private final RestClient restClient = RestClient.create();

    /**
     * 配信開始をDiscord Webhookへ通知する。
     * TwitchはチャンネルのトップページURL、YouTubeは配信動画そのもののURLを本文に含める。
     *
     * @param webhookUrl 通知先のDiscord Webhook URL
     * @param platform 配信プラットフォーム("twitch"または"youtube")
     * @param channelName チャンネルの表示名
     * @param channelLogin TwitchのURL用ログイン名(YouTubeの場合は未使用)
     * @param videoId 配信中の動画ID(Twitchの場合は未使用、nullを渡す)
     */
    public void sendStreamOnlineNotification(String webhookUrl, String platform, String channelName, String channelLogin, String videoId) {
        String streamUrl = "twitch".equals(platform)
                ? "https://www.twitch.tv/" + channelLogin
                : "https://www.youtube.com/watch?v=" + videoId;

        Map<String, Object> requestBody = Map.of(
                "content", "🔴 **" + channelName + "** が配信を開始しました!\n" + streamUrl
        );

        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}
package com.shin.streamnotify.streamer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 配信者(TwitchまたはYouTubeのチャンネル)を表すエンティティ。
 * 同じ配信者を複数のユーザーが登録している場合でも、Streamerレコードは1件のみ存在し、
 * Registration(中間テーブル)を通じてUserと多対多の関係を持つ。
 */
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

    // TwitchのURL用ログイン名(broadcaster_login)。表示名(channelName)とは別の識別子。
    // YouTubeはチャンネルIDがそのままURLに使われるため、YouTube側では使われない。
    @Column(name = "channel_login")
    private String channelLogin;

    // Twitch EventSubの購読ID。購読解除時にTwitch APIへ渡すために保持する。
    // YouTubeはPubSubHubbubの仕様上、購読IDに相当するものが発行されないため使われない。
    @Column(name = "twitch_subscription_id")
    private String twitchSubscriptionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 新しいStreamerを作成する。
     *
     * @param platform 配信プラットフォーム("twitch"または"youtube")
     * @param platformChannelId プラットフォーム内でのチャンネルID
     * @param channelName チャンネルの表示名
     * @param channelLogin TwitchのURL用ログイン名(YouTubeの場合はチャンネルIDと同じ値、または未使用)
     */
    public Streamer(String platform, String platformChannelId, String channelName, String channelLogin) {
        this.platform = platform;
        this.platformChannelId = platformChannelId;
        this.channelName = channelName;
        this.channelLogin = channelLogin;
        this.createdAt = LocalDateTime.now();
    }
}
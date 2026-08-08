package com.shin.streamnotify.streamer;

/**
 * 配信者登録(POST /streamers/register)のリクエストDTO。
 * 検索結果(ChannelSearchResult)から選ばれたチャンネルの情報を、
 * そのままこの形で登録エンドポイントへ渡す想定。
 *
 * @param platform 配信プラットフォーム("twitch"または"youtube")
 * @param platformChannelId プラットフォーム内でのチャンネルID
 * @param channelName チャンネルの表示名
 * @param channelLogin TwitchのURL用ログイン名(YouTubeの場合はチャンネルIDと同じ値)
 */
public record StreamerRegistrationRequest(
        String platform,
        String platformChannelId,
        String channelName,
        String channelLogin
) {
}
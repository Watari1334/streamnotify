package com.shin.streamnotify.streamer;

import java.time.LocalDateTime;

/**
 * 登録済み配信者一覧(GET /streamers)のレスポンスDTO。
 * Registrationエンティティをそのまま返さず、
 * フロントエンドが必要とする情報だけに絞って返すために使う。
 *
 * @param streamerId StreamerのID(削除時のパスパラメータとして使う)
 * @param platform 配信プラットフォーム("twitch"または"youtube")
 * @param platformChannelId プラットフォーム内でのチャンネルID
 * @param channelName チャンネルの表示名
 * @param registeredAt この配信者を登録した日時(Registrationのcreated_at)
 */
public record StreamerResponse(
        Long streamerId,
        String platform,
        String platformChannelId,
        String channelName,
        LocalDateTime registeredAt
) {
}
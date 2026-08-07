package com.shin.streamnotify.webhook;

/**
 * TwitchのEventSub Webhookが送ってくる通知本文のDTO。
 * messageTypeが"notification"の場合のリクエストボディに対応する。
 *
 * @param subscription 通知の元になった購読の情報
 * @param event 実際に発生したイベント(配信開始)の詳細
 */
public record TwitchEventSubNotification(
        Subscription subscription,
        StreamOnlineEvent event
) {

    /**
     * 通知の元になった購読の情報。
     *
     * @param id 購読ID
     * @param status 購読のステータス(例: "enabled")
     * @param type イベントの種類(例: "stream.online")
     */
    public record Subscription(
            String id,
            String status,
            String type
    ) {
    }

    /**
     * 配信開始イベントの詳細。
     *
     * @param id イベントID
     * @param broadcaster_user_id 配信者のTwitchユーザーID
     * @param broadcaster_user_login 配信者のログイン名(URLスラッグ)
     * @param broadcaster_user_name 配信者の表示名
     * @param type 配信の種類(例: "live")
     * @param started_at 配信開始時刻(ISO 8601形式)
     */
    public record StreamOnlineEvent(
            String id,
            String broadcaster_user_id,
            String broadcaster_user_login,
            String broadcaster_user_name,
            String type,
            String started_at
    ) {
    }
}
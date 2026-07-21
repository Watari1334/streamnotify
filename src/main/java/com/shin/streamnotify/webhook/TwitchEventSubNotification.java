package com.shin.streamnotify.webhook;

public record TwitchEventSubNotification(
        Subscription subscription,
        StreamOnlineEvent event
) {
    public record Subscription(
            String id,
            String status,
            String type
    ) {
    }

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

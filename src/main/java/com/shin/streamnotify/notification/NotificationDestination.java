package com.shin.streamnotify.notification;

import com.shin.streamnotify.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ユーザーごとの通知先を表すエンティティ。
 * 現状はDiscord Webhookのみに対応するが、notificationTypeフィールドで
 * 将来的に他の通知先(Slack等)への拡張を見込んだ設計になっている。
 * 1ユーザーにつき1件のみ登録できる想定。
 */
@Entity
@Table(name = "notification_destinations")
@Getter
@Setter
@NoArgsConstructor
public class NotificationDestination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "destination_id")
    private Long destinationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "discord_webhook_url", nullable = false)
    private String discordWebhookUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Discord通知先を新規作成する。
     * notificationTypeは常に"DISCORD"で固定される。
     *
     * @param user 通知先の持ち主となるユーザー
     * @param discordWebhookUrl 通知を送るDiscord Webhook URL
     */
    public NotificationDestination(User user, String discordWebhookUrl) {
        this.user = user;
        this.notificationType = "DISCORD";
        this.discordWebhookUrl = discordWebhookUrl;
        this.createdAt = LocalDateTime.now();
    }
}
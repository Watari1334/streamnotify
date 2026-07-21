package com.shin.streamnotify.notification;

import com.shin.streamnotify.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    public NotificationDestination(User user, String discordWebhookUrl) {
        this.user = user;
        this.notificationType = "DISCORD";
        this.discordWebhookUrl = discordWebhookUrl;
        this.createdAt = LocalDateTime.now();
    }
}
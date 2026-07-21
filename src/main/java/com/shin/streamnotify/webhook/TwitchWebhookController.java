package com.shin.streamnotify.webhook;

import tools.jackson.databind.ObjectMapper;
import com.shin.streamnotify.notification.DiscordNotificationService;
import com.shin.streamnotify.notification.NotificationDestination;
import com.shin.streamnotify.notification.NotificationDestinationRepository;
import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.streamer.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TwitchWebhookController {

    @Value("${twitch.eventsub.secret}")
    private String eventSubSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final StreamerRepository streamerRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationDestinationRepository notificationDestinationRepository;
    private final DiscordNotificationService discordNotificationService;

    @PostMapping("/webhooks/twitch")
    public ResponseEntity<String> handleTwitchWebhook(
            @RequestHeader("Twitch-Eventsub-Message-Id") String messageId,
            @RequestHeader("Twitch-Eventsub-Message-Timestamp") String timestamp,
            @RequestHeader("Twitch-Eventsub-Message-Signature") String signature,
            @RequestHeader("Twitch-Eventsub-Message-Type") String messageType,
            @RequestBody String body
    ) throws Exception {
        String expectedSignature = computeSignature(messageId, timestamp, body);
        if (!expectedSignature.equals(signature)) {
            return ResponseEntity.status(403).body("署名が一致しません");
        }

        if ("webhook_callback_verification".equals(messageType)) {
            TwitchChallengeRequest challengeRequest =
                    objectMapper.readValue(body, TwitchChallengeRequest.class);
            return ResponseEntity.ok(challengeRequest.challenge());
        }

        if ("notification".equals(messageType)) {
            handleStreamOnlineNotification(body);
            return ResponseEntity.ok("");
        }

        if ("revocation".equals(messageType)) {
            System.out.println("購読が取り消されました: " + body);
            return ResponseEntity.ok("");
        }

        return ResponseEntity.ok("");
    }

    private void handleStreamOnlineNotification(String body) throws Exception {
        TwitchEventSubNotification notification =
                objectMapper.readValue(body, TwitchEventSubNotification.class);

        String broadcasterUserId = notification.event().broadcaster_user_id();

        Optional<Streamer> streamerOpt = streamerRepository
                .findByPlatformAndPlatformChannelId("twitch", broadcasterUserId);

        if (streamerOpt.isEmpty()) {
            System.out.println("該当するStreamerが見つかりません: " + broadcasterUserId);
            return;
        }

        Streamer streamer = streamerOpt.get();

        List<Registration> registrations =
                registrationRepository.findByStreamer_StreamerId(streamer.getStreamerId());

        for (Registration registration : registrations) {
            Long userId = registration.getUser().getUserId();
            List<NotificationDestination> destinations =
                    notificationDestinationRepository.findByUser_UserId(userId);

            for (NotificationDestination destination : destinations) {
                discordNotificationService.sendStreamOnlineNotification(
                        destination.getDiscordWebhookUrl(),
                        streamer.getChannelName()
                );
            }
        }
    }

    private String computeSignature(String messageId, String timestamp, String body) {
        try {
            String message = messageId + timestamp + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    eventSubSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("署名計算に失敗しました", e);
        }
    }
}
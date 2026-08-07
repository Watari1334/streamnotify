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
import lombok.extern.slf4j.Slf4j;
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

/**
 * TwitchのEventSub Webhookを受信するコントローラ。
 * 署名検証(HMAC-SHA256)によって、リクエストが正規のTwitchからのものかを確認する。
 */
@Slf4j
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

    /**
     * TwitchのEventSub Webhookを受信するエンドポイント。
     * messageTypeによって処理を出し分ける。
     * - webhook_callback_verification: 購読登録時の検証チャレンジに応答する
     * - notification: 実際の配信開始通知を処理する
     * - revocation: Twitch側から購読が取り消されたことの通知
     *
     * @param messageId Twitchが発行するメッセージID(署名計算に使用)
     * @param timestamp メッセージのタイムスタンプ(署名計算に使用)
     * @param signature Twitchが計算した署名(HMAC-SHA256)
     * @param messageType メッセージの種類
     * @param body リクエストボディ(JSON文字列)
     * @return Twitchへのレスポンス。署名不一致の場合は403
     * @throws Exception JSONのパースに失敗した場合
     */
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
            log.warn("購読が取り消されました: {}", body);
            return ResponseEntity.ok("");
        }

        return ResponseEntity.ok("");
    }

    /**
     * 配信開始通知(notification)を処理する。
     * 通知対象のStreamerを特定し、登録している全ユーザーのDiscord Webhookへ通知を送る。
     *
     * @param body リクエストボディ(JSON文字列)
     * @throws Exception JSONのパースに失敗した場合
     */
    private void handleStreamOnlineNotification(String body) throws Exception {
        TwitchEventSubNotification notification =
                objectMapper.readValue(body, TwitchEventSubNotification.class);

        String broadcasterUserId = notification.event().broadcaster_user_id();

        Optional<Streamer> streamerOpt = streamerRepository
                .findByPlatformAndPlatformChannelId("twitch", broadcasterUserId);

        if (streamerOpt.isEmpty()) {
            log.warn("該当するStreamerが見つかりません: {}", broadcasterUserId);
            return;
        }

        Streamer streamer = streamerOpt.get();

        List<Registration> registrations =
                registrationRepository.findByStreamer_StreamerId(streamer.getStreamerId());

        for (Registration registration : registrations) {
            Long userId = registration.getUser().getUserId();
            Optional<NotificationDestination> destinationOpt =
                    notificationDestinationRepository.findByUser_UserId(userId);

            destinationOpt.ifPresent(destination ->
                    discordNotificationService.sendStreamOnlineNotification(
                            destination.getDiscordWebhookUrl(),
                            streamer.getPlatform(),
                            streamer.getChannelName(),
                            streamer.getChannelLogin(),
                            null
                    )
            );
        }
    }

    /**
     * Twitchから送られた署名(Twitch-Eventsub-Message-Signature)と比較するための
     * 期待値をHMAC-SHA256で計算する。
     * messageId + timestamp + bodyを、EventSub登録時に渡した秘密鍵で署名する。
     *
     * @param messageId Twitchが発行するメッセージID
     * @param timestamp メッセージのタイムスタンプ
     * @param body リクエストボディ
     * @return "sha256="で始まる16進数の署名文字列
     */
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
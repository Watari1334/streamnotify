package com.shin.streamnotify.webhook;

import com.shin.streamnotify.notification.DiscordNotificationService;
import com.shin.streamnotify.notification.NotificationDestination;
import com.shin.streamnotify.notification.NotificationDestinationRepository;
import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.streamer.StreamerRepository;
import com.shin.streamnotify.youtube.YouTubeEventSubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTubeのPubSubHubbub(WebSub) Webhookを受信するコントローラ。
 * 署名検証(HMAC-SHA1、X-Hub-Signatureヘッダー)によって、
 * リクエストが正規のGoogle PubSubHubbubサーバーからのものかを確認する。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class YouTubeWebhookController {

    private final StreamerRepository streamerRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationDestinationRepository notificationDestinationRepository;
    private final DiscordNotificationService discordNotificationService;
    private final YouTubeEventSubService youTubeEventSubService;
    private final StringRedisTemplate redisTemplate;

    @Value("${youtube.eventsub.secret}")
    private String eventSubSecret;

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("<yt:videoId>(.*?)</yt:videoId>");
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("<yt:channelId>(.*?)</yt:channelId>");
    private static final Duration NOTIFIED_TTL = Duration.ofHours(24);

    /**
     * PubSubHubbub購読登録時の検証チャレンジに応答する。
     *
     * @param challenge Googleから送られる検証用の文字列
     * @return challengeをそのまま返す
     */
    @GetMapping("/webhooks/youtube")
    public ResponseEntity<String> verify(
            @RequestParam("hub.challenge") String challenge
    ) {
        return ResponseEntity.ok(challenge);
    }

    /**
     * PubSubHubbubからの配信更新通知を受信するエンドポイント。
     * 署名を検証したうえで、実際にライブ配信中かどうかをYouTubeEventSubServiceで再確認し、
     * Redisで重複通知を防ぎながらDiscordへ通知を送る。
     *
     * @param signature Googleが計算した署名(HMAC-SHA1、X-Hub-Signatureヘッダー)
     * @param body リクエストボディ(Atom XML形式)
     * @return 常に200を返す。署名不一致の場合は403
     */
    @PostMapping("/webhooks/youtube")
    public ResponseEntity<Void> handleNotification(
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            @RequestBody String body
    ) {
        if (!isValidSignature(signature, body)) {
            log.warn("YouTube Webhookの署名が一致しません");
            return ResponseEntity.status(403).build();
        }

        Matcher videoIdMatcher = VIDEO_ID_PATTERN.matcher(body);
        Matcher channelIdMatcher = CHANNEL_ID_PATTERN.matcher(body);

        if (!videoIdMatcher.find() || !channelIdMatcher.find()) {
            return ResponseEntity.ok().build();
        }

        String videoId = videoIdMatcher.group(1);
        String channelId = channelIdMatcher.group(1);

        if (!youTubeEventSubService.isLive(videoId)) {
            return ResponseEntity.ok().build();
        }

        if (!tryMarkAsNotified(videoId)) {
            return ResponseEntity.ok().build();
        }

        handleStreamOnlineNotification(channelId, videoId);

        return ResponseEntity.ok().build();
    }

    /**
     * X-Hub-Signatureヘッダーの値が、リクエストボディから計算した署名と一致するか検証する。
     *
     * @param signature リクエストヘッダーの署名値("sha1=..."形式)。無ければnull
     * @param body リクエストボディ
     * @return 署名が一致すればtrue
     */
    private boolean isValidSignature(String signature, String body) {
        if (signature == null) {
            return false;
        }
        try {
            String expected = "sha1=" + computeHmacSha1(body);
            return expected.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("YouTube Webhookの署名計算に失敗しました", e);
            return false;
        }
    }

    /**
     * リクエストボディをHMAC-SHA1で署名し、16進数文字列として返す。
     *
     * @param body リクエストボディ
     * @return 16進数エンコードされた署名(sha1=は含まない)
     */
    private String computeHmacSha1(String body) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(
                eventSubSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"
        );
        mac.init(keySpec);
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * videoIdに対して初めての通知かどうかを判定し、初めてであれば通知済みとして記録する。
     * Redisのsetifabsent(SETNX)を使うことで、複数リクエストがほぼ同時に来ても
     * 通知が1回だけになることを保証する。
     *
     * @param videoId 対象のYouTube動画ID
     * @return 初めての通知であればtrue、既に通知済みであればfalse
     */
    private boolean tryMarkAsNotified(String videoId) {
        String key = "youtube:notified:" + videoId;
        Boolean isFirstNotification = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", NOTIFIED_TTL);
        return Boolean.TRUE.equals(isFirstNotification);
    }

    /**
     * 配信開始通知を処理する。通知対象のStreamerを特定し、
     * 登録している全ユーザーのDiscord Webhookへ通知を送る。
     *
     * @param channelId 配信者のYouTubeチャンネルID
     * @param videoId 配信中の動画ID
     */
    private void handleStreamOnlineNotification(String channelId, String videoId) {
        Optional<Streamer> streamerOpt = streamerRepository
                .findByPlatformAndPlatformChannelId("youtube", channelId);

        if (streamerOpt.isEmpty()) {
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
                            videoId
                    )
            );
        }
    }
}
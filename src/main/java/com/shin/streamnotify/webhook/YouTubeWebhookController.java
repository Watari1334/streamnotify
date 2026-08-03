package com.shin.streamnotify.webhook;

import com.shin.streamnotify.notification.DiscordNotificationService;
import com.shin.streamnotify.notification.NotificationDestination;
import com.shin.streamnotify.notification.NotificationDestinationRepository;
import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.streamer.StreamerRepository;
import com.shin.streamnotify.youtube.YouTubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
public class YouTubeWebhookController {

    private final StreamerRepository streamerRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationDestinationRepository notificationDestinationRepository;
    private final DiscordNotificationService discordNotificationService;
    private final YouTubeService youTubeService;

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("<yt:videoId>(.*?)</yt:videoId>");
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("<yt:channelId>(.*?)</yt:channelId>");

    @GetMapping("/webhooks/youtube")
    public ResponseEntity<String> verify(
            @RequestParam("hub.challenge") String challenge
    ) {
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/webhooks/youtube")
    public ResponseEntity<Void> handleNotification(@RequestBody String body) {
        Matcher videoIdMatcher = VIDEO_ID_PATTERN.matcher(body);
        Matcher channelIdMatcher = CHANNEL_ID_PATTERN.matcher(body);

        if (!videoIdMatcher.find() || !channelIdMatcher.find()) {
            return ResponseEntity.ok().build();
        }

        String videoId = videoIdMatcher.group(1);
        String channelId = channelIdMatcher.group(1);

        if (!youTubeService.isLive(videoId)) {
            return ResponseEntity.ok().build();
        }

        handleStreamOnlineNotification(channelId);

        return ResponseEntity.ok().build();
    }

    private void handleStreamOnlineNotification(String channelId) {
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
                            streamer.getChannelName(),
                            streamer.getChannelLogin()
                    )
            );
        }
    }
}
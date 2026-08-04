package com.shin.streamnotify.webhook;

import com.shin.streamnotify.notification.DiscordNotificationService;
import com.shin.streamnotify.notification.NotificationDestination;
import com.shin.streamnotify.notification.NotificationDestinationRepository;
import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.streamer.StreamerRepository;
import com.shin.streamnotify.user.User;
import com.shin.streamnotify.youtube.YouTubeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YouTubeWebhookControllerTest {

    @Mock private StreamerRepository streamerRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private NotificationDestinationRepository notificationDestinationRepository;
    @Mock private DiscordNotificationService discordNotificationService;
    @Mock private YouTubeService youTubeService;

    @InjectMocks
    private YouTubeWebhookController youTubeWebhookController;

    @Test
    void 検証リクエストにはchallengeをそのまま返す() {
        // Act
        ResponseEntity<String> result = youTubeWebhookController.verify("test-challenge-123");

        // Assert
        assertThat(result.getBody()).isEqualTo("test-challenge-123");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 配信中でなければDiscord通知は送られない() {
        // Arrange
        String xmlBody = """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015">
                  <entry>
                    <yt:videoId>video123</yt:videoId>
                    <yt:channelId>channel456</yt:channelId>
                  </entry>
                </feed>
                """;

        when(youTubeService.isLive("video123")).thenReturn(false);

        // Act
        youTubeWebhookController.handleNotification(xmlBody);

        // Assert
        verify(streamerRepository, never()).findByPlatformAndPlatformChannelId(anyString(), anyString());
    }

    @Test
    void 配信中ならDiscordに通知が送られる() {
        // Arrange
        String xmlBody = """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015">
                  <entry>
                    <yt:videoId>video123</yt:videoId>
                    <yt:channelId>channel456</yt:channelId>
                  </entry>
                </feed>
                """;

        Streamer streamer = new Streamer("youtube", "channel456", "テストチャンネル", "test_channel");

        User user = mock(User.class);
        when(user.getUserId()).thenReturn(1L);

        Registration registration = new Registration(user, streamer);

        NotificationDestination destination = mock(NotificationDestination.class);
        when(destination.getDiscordWebhookUrl()).thenReturn("https://discord.com/webhook/test");

        when(youTubeService.isLive("video123")).thenReturn(true);
        when(streamerRepository.findByPlatformAndPlatformChannelId("youtube", "channel456"))
                .thenReturn(Optional.of(streamer));
        when(registrationRepository.findByStreamer_StreamerId(streamer.getStreamerId()))
                .thenReturn(List.of(registration));
        when(notificationDestinationRepository.findByUser_UserId(1L))
                .thenReturn(Optional.of(destination));

        // Act
        youTubeWebhookController.handleNotification(xmlBody);

        // Assert
        verify(discordNotificationService).sendStreamOnlineNotification(
                "https://discord.com/webhook/test",
                "youtube",
                "テストチャンネル",
                "test_channel",
                "video123"
        );
    }

    @Test
    void 登録されていないチャンネルの配信は通知されない() {
        // Arrange
        String xmlBody = """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015">
                  <entry>
                    <yt:videoId>video123</yt:videoId>
                    <yt:channelId>unknown-channel</yt:channelId>
                  </entry>
                </feed>
                """;

        when(youTubeService.isLive("video123")).thenReturn(true);
        when(streamerRepository.findByPlatformAndPlatformChannelId("youtube", "unknown-channel"))
                .thenReturn(Optional.empty());

        // Act
        youTubeWebhookController.handleNotification(xmlBody);

        // Assert
        verify(discordNotificationService, never()).sendStreamOnlineNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
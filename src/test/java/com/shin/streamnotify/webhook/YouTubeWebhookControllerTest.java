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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YouTubeWebhookControllerTest {

    private static final String TEST_SECRET = "test-secret";

    @Mock private StreamerRepository streamerRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private NotificationDestinationRepository notificationDestinationRepository;
    @Mock private DiscordNotificationService discordNotificationService;
    @Mock private YouTubeService youTubeService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private YouTubeWebhookController youTubeWebhookController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(youTubeWebhookController, "eventSubSecret", TEST_SECRET);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
    }

    /**
     * テスト対象のコントローラと同じアルゴリズムで、テスト用の正しい署名を作る。
     */
    private String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return "sha1=" + HexFormat.of().formatHex(hash);
    }

    @Test
    void 検証リクエストにはchallengeをそのまま返す() {
        // Act
        ResponseEntity<String> result = youTubeWebhookController.verify("test-challenge-123");

        // Assert
        assertThat(result.getBody()).isEqualTo("test-challenge-123");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 署名が不正な場合は403を返す() {
        // Arrange
        String xmlBody = """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015">
                  <entry>
                    <yt:videoId>video123</yt:videoId>
                    <yt:channelId>channel456</yt:channelId>
                  </entry>
                </feed>
                """;

        // Act
        ResponseEntity<Void> result =
                youTubeWebhookController.handleNotification("sha1=invalid", xmlBody);

        // Assert
        assertThat(result.getStatusCode().value()).isEqualTo(403);
        verify(streamerRepository, never()).findByPlatformAndPlatformChannelId(anyString(), anyString());
    }

    @Test
    void 署名ヘッダーが無い場合は403を返す() {
        // Arrange
        String xmlBody = """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015">
                  <entry>
                    <yt:videoId>video123</yt:videoId>
                    <yt:channelId>channel456</yt:channelId>
                  </entry>
                </feed>
                """;

        // Act
        ResponseEntity<Void> result =
                youTubeWebhookController.handleNotification(null, xmlBody);

        // Assert
        assertThat(result.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void 配信中でなければDiscord通知は送られない() throws Exception {
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
        youTubeWebhookController.handleNotification(sign(xmlBody), xmlBody);

        // Assert
        verify(streamerRepository, never()).findByPlatformAndPlatformChannelId(anyString(), anyString());
    }

    @Test
    void 配信中ならDiscordに通知が送られる() throws Exception {
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
        youTubeWebhookController.handleNotification(sign(xmlBody), xmlBody);

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
    void 登録されていないチャンネルの配信は通知されない() throws Exception {
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
        youTubeWebhookController.handleNotification(sign(xmlBody), xmlBody);

        // Assert
        verify(discordNotificationService, never()).sendStreamOnlineNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
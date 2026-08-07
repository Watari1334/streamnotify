package com.shin.streamnotify.streamer;

import com.shin.streamnotify.ratelimit.RateLimitService;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.twitch.TwitchEventSubService;
import com.shin.streamnotify.user.CurrentUserResolver;
import com.shin.streamnotify.user.User;
import com.shin.streamnotify.youtube.YouTubeEventSubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import com.shin.streamnotify.registration.Registration;

import java.time.Duration;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerControllerTest {

    @Mock private StreamerRepository streamerRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private TwitchEventSubService twitchEventSubService;
    @Mock private YouTubeEventSubService youTubeEventSubService;
    @Mock private RateLimitService rateLimitService;
    @Mock private OidcUser oidcUser;
    @Mock private User currentUser;

    @InjectMocks
    private StreamerController streamerController;

    @Test
    void 新規配信者を登録するとTwitchのサブスクリプションが作成される() {
        // Arrange
        StreamerRegistrationRequest request =
                new StreamerRegistrationRequest("twitch", "12345", "テストチャンネル", "test_channel");

        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(streamerRepository.findByPlatformAndPlatformChannelId("twitch", "12345"))
                .thenReturn(Optional.empty());

        Streamer savedStreamer = new Streamer("twitch", "12345", "テストチャンネル", "test_channel");
        when(streamerRepository.save(any(Streamer.class))).thenReturn(savedStreamer);

        when(twitchEventSubService.subscribeToStreamOnline("12345"))
                .thenReturn("sub-id-999");

        // Act
        String result = streamerController.registerStreamer(oidcUser, request);

        // Assert
        verify(twitchEventSubService).subscribeToStreamOnline("12345");
        verify(streamerRepository, times(2)).save(any(Streamer.class));
        assertThat(result).contains("テストチャンネル");
    }

    @Test
    void 既存の配信者に登録すると新規作成やTwitch購読は行われない() {
        // Arrange
        StreamerRegistrationRequest request =
                new StreamerRegistrationRequest("twitch", "12345", "テストチャンネル", "test_channel");

        Streamer existingStreamer = new Streamer("twitch", "12345", "テストチャンネル", "test_channel");

        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(streamerRepository.findByPlatformAndPlatformChannelId("twitch", "12345"))
                .thenReturn(Optional.of(existingStreamer));

        // Act
        String result = streamerController.registerStreamer(oidcUser, request);

        // Assert
        verify(streamerRepository, never()).save(any(Streamer.class));
        verify(twitchEventSubService, never()).subscribeToStreamOnline(anyString());
        verify(registrationRepository).save(any());
        assertThat(result).contains("テストチャンネル");
    }

    @Test
    void 存在しないユーザーで登録しようとすると例外が発生する() {
        // Arrange
        StreamerRegistrationRequest request =
                new StreamerRegistrationRequest("twitch", "12345", "テストチャンネル", "test_channel");

        when(currentUserResolver.resolve(oidcUser))
                .thenThrow(new IllegalStateException("ユーザーが見つかりません"));

        // Act & Assert
        assertThatThrownBy(() -> streamerController.registerStreamer(oidcUser, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ユーザーが見つかりません");

        verify(streamerRepository, never()).findByPlatformAndPlatformChannelId(anyString(), anyString());
    }

    @Test
    void 最後の登録者が削除するとTwitch購読解除とStreamer削除が行われる() {
        // Arrange
        Long streamerId = 1L;
        Long userId = 100L;

        Streamer streamer = new Streamer("twitch", "12345", "テストチャンネル", "test_channel");
        streamer.setTwitchSubscriptionId("sub-id-999");

        Registration registration = new Registration(currentUser, streamer);

        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(currentUser.getUserId()).thenReturn(userId);

        when(registrationRepository.findByUser_UserIdAndStreamer_StreamerId(userId, streamerId))
                .thenReturn(Optional.of(registration));

        when(registrationRepository.findByStreamer_StreamerId(streamerId))
                .thenReturn(List.of());

        // Act
        String result = streamerController.deleteStreamer(oidcUser, streamerId);

        // Assert
        verify(registrationRepository).delete(registration);
        verify(twitchEventSubService).unsubscribe("sub-id-999");
        verify(streamerRepository).delete(streamer);
        assertThat(result).isEqualTo("削除しました");
    }

    @Test
    void 他にも登録者がいる場合はStreamerもTwitch購読も削除されない() {
        // Arrange
        Long streamerId = 1L;
        Long userId = 100L;

        Streamer streamer = new Streamer("twitch", "12345", "テストチャンネル", "test_channel");
        streamer.setTwitchSubscriptionId("sub-id-999");

        Registration registration = new Registration(currentUser, streamer);
        Registration anotherRegistration = new Registration(currentUser, streamer);

        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(currentUser.getUserId()).thenReturn(userId);

        when(registrationRepository.findByUser_UserIdAndStreamer_StreamerId(userId, streamerId))
                .thenReturn(Optional.of(registration));

        when(registrationRepository.findByStreamer_StreamerId(streamerId))
                .thenReturn(List.of(anotherRegistration));

        // Act
        String result = streamerController.deleteStreamer(oidcUser, streamerId);

        // Assert
        verify(registrationRepository).delete(registration);
        verify(twitchEventSubService, never()).unsubscribe(anyString());
        verify(streamerRepository, never()).delete(any(Streamer.class));
        assertThat(result).isEqualTo("削除しました");
    }

    @Test
    void 自分の登録が見つからない場合は例外が発生する() {
        // Arrange
        Long streamerId = 1L;
        Long userId = 100L;

        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(currentUser.getUserId()).thenReturn(userId);

        when(registrationRepository.findByUser_UserIdAndStreamer_StreamerId(userId, streamerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> streamerController.deleteStreamer(oidcUser, streamerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("登録が見つかりません");

        verify(registrationRepository, never()).delete(any());
        verify(streamerRepository, never()).delete(any(Streamer.class));
        verify(twitchEventSubService, never()).unsubscribe(anyString());
    }

    @Test
    void YouTube検索は上限内なら成功する() {
        // Arrange
        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(currentUser.getUserId()).thenReturn(100L);
        when(rateLimitService.tryAcquire(eq("search:youtube:100"), eq(20L), any(Duration.class)))
                .thenReturn(true);
        when(youTubeEventSubService.searchChannels("query"))
                .thenReturn(List.of());

        // Act
        List<YouTubeEventSubService.ChannelSearchResult> result =
                streamerController.searchYouTubeStreamers(oidcUser, "query");

        // Assert
        assertThat(result).isEmpty();
        verify(youTubeEventSubService).searchChannels("query");
    }

    @Test
    void YouTube検索は上限を超えると例外が発生する() {
        // Arrange
        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(currentUser.getUserId()).thenReturn(100L);
        when(rateLimitService.tryAcquire(eq("search:youtube:100"), eq(20L), any(Duration.class)))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> streamerController.searchYouTubeStreamers(oidcUser, "query"))
                .isInstanceOf(SearchLimitExceededException.class);

        verify(youTubeEventSubService, never()).searchChannels(anyString());
    }
}
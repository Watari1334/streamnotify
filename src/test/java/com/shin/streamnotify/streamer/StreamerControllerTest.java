package com.shin.streamnotify.streamer;

import com.shin.streamnotify.ratelimit.RateLimitService;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.twitch.TwitchEventSubService;
import com.shin.streamnotify.user.CurrentUserResolver;
import com.shin.streamnotify.user.User;
import com.shin.streamnotify.youtube.YouTubeService;
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
    @Mock private YouTubeService youTubeService;
    @Mock private RateLimitService rateLimitService;
    @Mock private OidcUser oidcUser;
    @Mock private User currentUser;

    @InjectMocks
    private StreamerController streamerController;

    // ...(既存の5テストはそのまま)

    @Test
    void YouTube検索は上限内なら成功する() {
        // Arrange
        when(currentUserResolver.resolve(oidcUser)).thenReturn(currentUser);
        when(currentUser.getUserId()).thenReturn(100L);
        when(rateLimitService.tryAcquire(eq("search:youtube:100"), eq(20L), any(Duration.class)))
                .thenReturn(true);
        when(youTubeService.searchChannels("query"))
                .thenReturn(List.of());

        // Act
        List<YouTubeService.ChannelSearchResult> result =
                streamerController.searchYouTubeStreamers(oidcUser, "query");

        // Assert
        assertThat(result).isEmpty();
        verify(youTubeService).searchChannels("query");
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

        verify(youTubeService, never()).searchChannels(anyString());
    }
}
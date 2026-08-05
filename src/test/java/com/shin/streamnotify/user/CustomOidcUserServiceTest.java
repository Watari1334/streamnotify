package com.shin.streamnotify.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private OidcUser oidcUser;

    @InjectMocks
    private CustomOidcUserService customOidcUserService;

    @Test
    void 初回ログインで新しいUserが作られる() {
        // Arrange
        when(oidcUser.getSubject()).thenReturn("twitch-subject-123");
        when(currentUserResolver.resolveProvider(oidcUser)).thenReturn("twitch");
        when(currentUserResolver.resolveDisplayName(oidcUser)).thenReturn("わたり");

        when(userRepository.findByOauthProviderAndOauthSubject("twitch", "twitch-subject-123"))
                .thenReturn(Optional.empty());

        // Act
        customOidcUserService.provisionUser(oidcUser);

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getUserName().equals("わたり")
                        && user.getOauthSubject().equals("twitch-subject-123")
                        && user.getOauthProvider().equals("twitch")
        ));
    }

    @Test
    void 既存ユーザーがログインしても新しいUserは作られない() {
        // Arrange
        User existingUser = new User("わたり", "twitch", "twitch-subject-123");

        when(oidcUser.getSubject()).thenReturn("twitch-subject-123");
        when(currentUserResolver.resolveProvider(oidcUser)).thenReturn("twitch");

        when(userRepository.findByOauthProviderAndOauthSubject("twitch", "twitch-subject-123"))
                .thenReturn(Optional.of(existingUser));

        // Act
        customOidcUserService.provisionUser(oidcUser);

        // Assert
        verify(userRepository, never()).save(any(User.class));
    }
}
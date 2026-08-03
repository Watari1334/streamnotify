package com.shin.streamnotify.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public User resolve(OidcUser oidcUser) {
        String provider = resolveProvider(oidcUser);
        return userRepository.findByOauthProviderAndOauthSubject(provider, oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }

    public String resolveProvider(OidcUser oidcUser) {
        String issuer = oidcUser.getIssuer().toString();
        if (issuer.contains("twitch")) {
            return "twitch";
        }
        if (issuer.contains("google")) {
            return "google";
        }
        return "unknown";
    }
}
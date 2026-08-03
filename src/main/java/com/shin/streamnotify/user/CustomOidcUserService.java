package com.shin.streamnotify.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        provisionUser(oidcUser);
        return oidcUser;
    }

    void provisionUser(OidcUser oidcUser) {
        String provider = currentUserResolver.resolveProvider(oidcUser);
        String subject = oidcUser.getSubject();

        userRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> {
                    User newUser = new User(
                            resolveUserName(oidcUser, provider, subject),
                            provider,
                            subject
                    );
                    return userRepository.save(newUser);
                });
    }

    private String resolveUserName(OidcUser oidcUser, String provider, String subject) {
        String preferredUsername = oidcUser.getPreferredUsername();
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return provider + "-user-" + subject;
    }
}
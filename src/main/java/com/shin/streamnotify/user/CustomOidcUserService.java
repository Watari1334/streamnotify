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

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String twitchSubject = oidcUser.getSubject();

        userRepository.findByTwitchSubject(twitchSubject)
                .orElseGet(() -> {
                    User newUser = new User(
                            resolveUserName(oidcUser, twitchSubject),
                            twitchSubject
                    );
                    return userRepository.save(newUser);
                });

        return oidcUser;
    }

    private String resolveUserName(OidcUser oidcUser, String twitchSubject) {
        String preferredUsername = oidcUser.getPreferredUsername();
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return "twitch-user-" + twitchSubject;
    }
}
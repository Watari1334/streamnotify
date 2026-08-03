package com.shin.streamnotify.web;

import com.shin.streamnotify.user.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OidcUser oidcUser) {
        String displayName = oidcUser.getPreferredUsername();
        if (displayName == null || displayName.isBlank()) {
            String provider = currentUserResolver.resolveProvider(oidcUser);
            displayName = provider + "-user-" + oidcUser.getSubject();
        }
        return new MeResponse(displayName);
    }

    record MeResponse(String displayName) {}
}
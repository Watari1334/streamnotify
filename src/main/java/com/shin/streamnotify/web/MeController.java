package com.shin.streamnotify.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OidcUser oidcUser) {
        String displayName = oidcUser.getPreferredUsername();
        if (displayName == null || displayName.isBlank()) {
            displayName = "twitch-user-" + oidcUser.getSubject();
        }
        return new MeResponse(displayName);
    }

    record MeResponse(String displayName) {}
}
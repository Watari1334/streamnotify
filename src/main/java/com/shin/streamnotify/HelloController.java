package com.shin.streamnotify;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return "ログインしていません";
        }
        return "ログイン成功! ユーザーID: " + oidcUser.getSubject();
    }
}
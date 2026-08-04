package com.shin.streamnotify.web;

import com.shin.streamnotify.user.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ログイン中のユーザー情報をフロントエンドに返すコントローラ。
 */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserResolver currentUserResolver;

    /**
     * ログイン中のユーザーの表示名を返す。
     * OIDCのpreferred_usernameクレームが無い場合は、
     * "プロバイダ名-user-サブジェクトID"の形式で代替の表示名を組み立てる。
     *
     * @param oidcUser Spring Securityが自動注入する認証済みユーザー情報
     * @return 表示名を含むレスポンス
     */
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OidcUser oidcUser) {
        String displayName = oidcUser.getPreferredUsername();
        if (displayName == null || displayName.isBlank()) {
            String provider = currentUserResolver.resolveProvider(oidcUser);
            displayName = provider + "-user-" + oidcUser.getSubject();
        }
        return new MeResponse(displayName);
    }

    /**
     * /api/meのレスポンス用DTO。
     *
     * @param displayName ユーザーの表示名
     */
    record MeResponse(String displayName) {}
}
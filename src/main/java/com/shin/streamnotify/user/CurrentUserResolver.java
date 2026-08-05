package com.shin.streamnotify.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * OIDC認証情報(OidcUser)から、アプリ内のUserエンティティを解決するクラス。
 * TwitchとGoogleの両方のOIDCログインに対応する。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    /**
     * OidcUserに対応するUserエンティティをDBから取得する。
     * 該当ユーザーが存在しない場合は例外を投げる。
     *
     * @param oidcUser Spring Securityが管理する認証済みユーザー情報
     * @return DBに保存されているUserエンティティ
     * @throws IllegalStateException 該当ユーザーがDBに存在しない場合
     */
    public User resolve(OidcUser oidcUser) {
        String provider = resolveProvider(oidcUser);
        return userRepository.findByOauthProviderAndOauthSubject(provider, oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }

    /**
     * OidcUserのissuer(iss)クレームから、ログインに使われたプロバイダを判定する。
     * 判定できない場合は"unknown"を返す。
     *
     * @param oidcUser Spring Securityが管理する認証済みユーザー情報
     * @return "twitch"、"google"、またはどちらでもない場合は"unknown"
     */
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

    /**
     * OidcUserのpreferred_usernameクレームから表示名を決定する。
     * クレームが無い場合は"プロバイダ名-user-サブジェクトID"を仮の表示名とする。
     *
     * @param oidcUser 認証済みのOIDCユーザー情報
     * @return 決定された表示名
     */
    public String resolveDisplayName(OidcUser oidcUser) {
        String preferredUsername = oidcUser.getPreferredUsername();
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        String provider = resolveProvider(oidcUser);
        return provider + "-user-" + oidcUser.getSubject();
    }
}
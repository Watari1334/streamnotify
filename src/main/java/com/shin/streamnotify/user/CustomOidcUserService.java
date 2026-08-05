package com.shin.streamnotify.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * OIDCログイン成功時のフック。認証情報の取得に加えて、
 * 初回ログイン時にアプリ内のUserレコードを自動作成する。
 */
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

    /**
     * OidcUserに対応するUserレコードがDBに存在しなければ、新規作成する。
     * 既に存在する場合は何もしない。
     *
     * @param oidcUser 認証済みのOIDCユーザー情報
     */
    void provisionUser(OidcUser oidcUser) {
        String provider = currentUserResolver.resolveProvider(oidcUser);
        String subject = oidcUser.getSubject();

        boolean userExists = userRepository
                .findByOauthProviderAndOauthSubject(provider, subject)
                .isPresent();

        if (!userExists) {
            User newUser = new User(
                    currentUserResolver.resolveDisplayName(oidcUser),
                    provider,
                    subject
            );
            userRepository.save(newUser);
        }
    }
}
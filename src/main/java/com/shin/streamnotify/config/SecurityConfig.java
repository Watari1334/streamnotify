package com.shin.streamnotify.config;

import com.shin.streamnotify.user.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Securityの設定。認証・認可・CSRF対策・OIDCログインをまとめて構成する。
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    /**
     * アプリ全体のセキュリティフィルタチェーンを構築する。
     *
     * CSRF対策: CookieCsrfTokenRepositoryを使い、SPA向けにトークンをCookie経由で配布する
     * (/api/csrf-tokenで取得し、以降のリクエストヘッダーに付与する運用)。
     * /webhooks/**はTwitch/YouTubeからの直接POSTを受けるため、CSRF検証の対象から除外する。
     *
     * 認可: /webhooks/**、トップページ、/api/csrf-token、ヘルスチェックのみ未認証で許可し、
     * それ以外の全リクエストは認証必須とする。
     *
     * ログイン: OIDC(Twitch/Google)でログインし、成功後はdashboard.htmlへ遷移する。
     * ユーザー情報の取得にはCustomOidcUserServiceを使い、
     * 初回ログイン時のUserレコード自動作成を行う。
     *
     * @param http Spring Securityが提供するセキュリティ設定用のビルダー
     * @return 構築されたセキュリティフィルタチェーン
     * @throws Exception 設定の構築に失敗した場合
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieCsrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/webhooks/**")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/webhooks/**").permitAll()
                        .requestMatchers("/", "/index.html", "/api/csrf-token").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/dashboard.html", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
                        )
                );

        return http.build();
    }
}
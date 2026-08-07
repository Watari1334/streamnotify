package com.shin.streamnotify.twitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Twitch APIを呼ぶためのアプリアクセストークンを取得するサービス。
 * OAuth2のクライアントクレデンシャルズフロー(grant_type=client_credentials)を使い、
 * 特定ユーザーではなくアプリ自体の権限でトークンを取得する。
 * ユーザーのTwitchログイン(authorization_code方式)とは別の仕組み。
 * トークンは有効期限内であればインスタンス内にキャッシュし、
 * 呼び出しのたびにTwitchへリクエストを送らないようにしている。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwitchAuthService {

    private final RestClient restClient = RestClient.create();

    @Value("${spring.security.oauth2.client.registration.twitch.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.twitch.client-secret}")
    private String clientSecret;

    // 有効期限ぎりぎりでの失効を避けるための余裕(秒)
    private static final long EXPIRY_MARGIN_SECONDS = 60;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt;

    /**
     * Twitchのアプリアクセストークンを取得する。
     * キャッシュ済みのトークンが有効期限内であればそれを返し、
     * 無ければ(または期限切れなら)新規に取得してキャッシュする。
     *
     * @return Twitch API呼び出しに使うアクセストークン
     */
    public synchronized String getAppAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");

        TokenResponse response = restClient.post()
                .uri("https://id.twitch.tv/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        cachedToken = response.accessToken();
        tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn() - EXPIRY_MARGIN_SECONDS);

        log.info("Twitchアプリアクセストークンを新規取得しました。有効期限: {}", tokenExpiresAt);

        return cachedToken;
    }

    /**
     * Twitchトークンエンドポイントのレスポンス。
     *
     * @param access_token 発行されたアクセストークン
     * @param expires_in トークンの有効期限(秒)
     * @param token_type トークンの種類(通常"bearer")
     */
    private record TokenResponse(
            String access_token,
            Integer expires_in,
            String token_type
    ) {
        String accessToken() {
            return access_token;
        }

        long expiresIn() {
            return expires_in;
        }
    }
}
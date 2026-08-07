package com.shin.streamnotify.twitch;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Twitch APIを呼ぶためのアプリアクセストークンを取得するサービス。
 * OAuth2のクライアントクレデンシャルズフロー(grant_type=client_credentials)を使い、
 * 特定ユーザーではなくアプリ自体の権限でトークンを取得する。
 * ユーザーのTwitchログイン(authorization_code方式)とは別の仕組み。
 */
@Service
@RequiredArgsConstructor
public class TwitchAuthService {

    private final RestClient restClient = RestClient.create();

    @Value("${spring.security.oauth2.client.registration.twitch.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.twitch.client-secret}")
    private String clientSecret;

    /**
     * Twitchのアプリアクセストークンを新規取得する。
     * 呼び出しのたびにTwitchの認証サーバーへリクエストを送る
     * (トークンの有効期限内での使い回し・キャッシュは行っていない)。
     *
     * @return Twitch API呼び出しに使うアクセストークン
     */
    public String getAppAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        TokenResponse response = restClient.post()
                .uri("https://id.twitch.tv/oauth2/token")
                .headers(h -> h.addAll(headers))
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        return response.accessToken();
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
    }
}
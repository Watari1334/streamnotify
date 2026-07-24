package com.shin.streamnotify.twitch;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TwitchAuthService{

    private final RestClient restClient = RestClient.create();

    @Value("${spring.security.oauth2.client.registration.twitch.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.twitch.client-secret}")
    private String clientSecret;

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
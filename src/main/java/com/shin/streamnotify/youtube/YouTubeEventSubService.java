package com.shin.streamnotify.youtube;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
public class YouTubeEventSubService {

    private final RestClient restClient = RestClient.create();

    @Value("${youtube.eventsub.secret}")
    private String eventSubSecret;

    @Value("${youtube.eventsub.callback-url}")
    private String callbackUrl;

    private static final String HUB_URL = "https://pubsubhubbub.appspot.com/subscribe";

    public void subscribe(String channelId) {
        String topicUrl = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + channelId;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("hub.mode", "subscribe");
        formData.add("hub.topic", topicUrl);
        formData.add("hub.callback", callbackUrl);
        formData.add("hub.verify", "sync");
        formData.add("hub.secret", eventSubSecret);
        formData.add("hub.lease_seconds", "432000");

        ResponseEntity<String> response = restClient.post()
                .uri(HUB_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(String.class);

        System.out.println("PubSubHubbub購読結果、ステータス: " + response.getStatusCode());
    }

    public void unsubscribe(String channelId) {
        String topicUrl = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + channelId;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("hub.mode", "unsubscribe");
        formData.add("hub.topic", topicUrl);
        formData.add("hub.callback", callbackUrl);

        restClient.post()
                .uri(HUB_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();
    }
}
package com.shin.streamnotify.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final RestClient restClient = RestClient.create();

    @Value("${youtube.api.key}")
    private String apiKey;

    @Cacheable(value = "youtubeChannelSearch", key = "#query")
    public List<ChannelSearchResult> searchChannels(String query) {
        SearchResponse response = restClient.get()
                .uri("https://www.googleapis.com/youtube/v3/search?part=snippet&type=channel&q=" + query + "&key=" + apiKey)
                .retrieve()
                .body(SearchResponse.class);

        return response.items().stream()
                .map(item -> new ChannelSearchResult(
                        item.id().channelId(),
                        item.snippet().channelTitle(),
                        item.snippet().thumbnails().thumbnailDefault().url()
                ))
                .toList();
    }

    private record SearchResponse(List<SearchItem> items) {
    }

    private record SearchItem(SearchItemId id, Snippet snippet) {
    }

    private record SearchItemId(String channelId) {
    }

    private record Snippet(String channelTitle, Thumbnails thumbnails) {
    }

    private record Thumbnails(@JsonProperty("default") Thumbnail thumbnailDefault) {
    }

    private record Thumbnail(String url) {
    }

    public record ChannelSearchResult(
            String channelId,
            String channelTitle,
            String thumbnailUrl
    ) {
    }
}
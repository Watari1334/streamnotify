package com.shin.streamnotify.youtube;

import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.streamer.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class YouTubeResubscribeScheduler {

    private final StreamerRepository streamerRepository;
    private final YouTubeEventSubService youTubeEventSubService;

    @Scheduled(fixedRate = (long) (4.5 * 24 * 60 * 60 * 1000))
    public void resubscribeAll() {
        List<Streamer> youtubeStreamers = streamerRepository.findByPlatform("youtube");

        for (Streamer streamer : youtubeStreamers) {
            youTubeEventSubService.subscribe(streamer.getPlatformChannelId());
        }

        System.out.println("YouTube再購読、完了: " + youtubeStreamers.size() + "件");
    }
}
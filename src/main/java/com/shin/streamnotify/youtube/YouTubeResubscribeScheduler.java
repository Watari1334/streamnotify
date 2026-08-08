package com.shin.streamnotify.youtube;

import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.streamer.StreamerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * YouTubeのPubSubHubbub購読を、期限切れ前に自動で再購読するスケジューラ。
 * PubSubHubbubの購読は最大5日(432000秒、YouTubeEventSubService.subscribeのhub.lease_seconds)で
 * 失効するため、それより短い4.5日周期で再購読をかけることで、
 * 購読が切れて通知が届かなくなる事態を防いでいる。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeResubscribeScheduler {

    private final StreamerRepository streamerRepository;
    private final YouTubeEventSubService youTubeEventSubService;

    /**
     * 登録されている全YouTube配信者に対して、PubSubHubbub購読を再実行する。
     * 4.5日(390,000,000ミリ秒)ごとに自動実行される。
     * 1件の再購読が失敗しても、そのエラーをログに残したうえで、
     * 残りの配信者への再購読処理は継続する。
     */
    @Scheduled(fixedRate = (long) (4.5 * 24 * 60 * 60 * 1000))
    public void resubscribeAll() {
        List<Streamer> youtubeStreamers = streamerRepository.findByPlatform("youtube");

        int successCount = 0;
        for (Streamer streamer : youtubeStreamers) {
            try {
                youTubeEventSubService.subscribe(streamer.getPlatformChannelId());
                successCount++;
            } catch (Exception e) {
                log.error("YouTube再購読に失敗しました。channelId={}", streamer.getPlatformChannelId(), e);
            }
        }

        log.info("YouTube再購読、完了: {}/{}件", successCount, youtubeStreamers.size());
    }
}
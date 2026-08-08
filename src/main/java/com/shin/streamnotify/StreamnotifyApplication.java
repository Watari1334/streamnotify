package com.shin.streamnotify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * StreamNotifyアプリケーションのエントリーポイント。
 * TwitchとYouTubeの配信開始をDiscordに通知するアプリケーション本体。
 * EnableSchedulingにより、YouTubeResubscribeSchedulerなどの定期実行タスクが有効になる。
 * EnableCachingはCacheConfigに、購読管理の詳細はTwitchEventSubService/
 * YouTubeEventSubServiceに切り出されている。
 */
@SpringBootApplication
@EnableScheduling
public class StreamnotifyApplication {

    /**
     * アプリケーションを起動する。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(StreamnotifyApplication.class, args);
    }

}
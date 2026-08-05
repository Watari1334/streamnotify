package com.shin.streamnotify.config;

import com.shin.streamnotify.twitch.TwitchEventSubService.ChannelSearchResult;
import com.shin.streamnotify.youtube.YouTubeService;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis(Valkey)を使ったキャッシュの設定。
 * Twitch/YouTubeのチャンネル検索結果をキャッシュすることで、
 * 特にYouTube Data APIのクォータ消費(search.listは1回100ユニット、
 * 1日10,000ユニットが上限)を抑える目的がある。
 *
 * 値の保存形式にはあえてJDK標準のバイナリ直列化ではなくJSON形式を選んでいるため、
 * Javaのジェネリクス(List&lt;T&gt;)は型消去により実行時に型情報が失われる。
 * そのためキャッシュからの復元時に型を正しく戻せるよう、
 * JavaTypeを明示的に組み立ててシリアライザに渡している。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Twitch/YouTubeの検索結果キャッシュを管理するRedisCacheManagerを構築する。
     * デフォルトはTTL5分。YouTube検索(youtubeChannelSearch)のみ、
     * クォータの厳しさを考慮してTTLを24時間に延長している。
     *
     * @param connectionFactory Redis(Valkey)への接続情報。application.propertiesの
     *                          spring.data.redis設定からSpring Bootが自動構成する
     * @return TwitchとYouTubeそれぞれ専用の設定を持つRedisCacheManager
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                );

        JavaType channelSearchListType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, ChannelSearchResult.class);

        RedisCacheConfiguration channelSearchConfig = defaultConfig.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(channelSearchListType)
                )
        );

        JavaType youtubeChannelSearchListType = TypeFactory.createDefaultInstance()
                .constructCollectionType(List.class, YouTubeService.ChannelSearchResult.class);

        RedisCacheConfiguration youtubeChannelSearchConfig = defaultConfig
                .entryTtl(Duration.ofHours(24))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(youtubeChannelSearchListType)
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(
                        Map.of(
                                "twitchchannelSearch", channelSearchConfig,
                                "youtubeChannelSearch", youtubeChannelSearchConfig
                        )
                )
                .build();
    }
}
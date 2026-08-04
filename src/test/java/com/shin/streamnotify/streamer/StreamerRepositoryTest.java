package com.shin.streamnotify.streamer;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StreamerRepositoryTest {

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private StreamerRepository streamerRepository;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void 配信者を保存して検索できる() {
        Streamer streamer = new Streamer("twitch", "1264206199", "自分のチャンネル", "my_channel");
        streamerRepository.save(streamer);

        Optional<Streamer> found = streamerRepository
                .findByPlatformAndPlatformChannelId("twitch", "1264206199");

        assertThat(found).isPresent();
        assertThat(found.get().getChannelName()).isEqualTo("自分のチャンネル");
    }
}
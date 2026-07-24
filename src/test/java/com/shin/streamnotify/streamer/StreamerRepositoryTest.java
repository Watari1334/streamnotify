package com.shin.streamnotify.streamer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StreamerRepositoryTest {

    @Autowired
    private StreamerRepository streamerRepository;

    @Test
    void 配信者を保存して検索できる() {
        Streamer streamer = new Streamer("twitch", "1264206199", "自分のチャンネル");
        streamerRepository.save(streamer);

        Optional<Streamer> found = streamerRepository
                .findByPlatformAndPlatformChannelId("twitch", "1264206199");

        assertThat(found).isPresent();
        assertThat(found.get().getChannelName()).isEqualTo("自分のチャンネル");
    }
}
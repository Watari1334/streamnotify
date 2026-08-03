package com.shin.streamnotify;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("ローカル環境からは、RDSに、接続できないため、スキップ")
class StreamnotifyApplicationTests {

    @Test
    void contextLoads() {
    }

}
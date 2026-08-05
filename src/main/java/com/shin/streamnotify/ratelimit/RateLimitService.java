package com.shin.streamnotify.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * ユーザー単位でのAPI呼び出し回数を制限するサービス。
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 指定したキーに対する呼び出し回数をインクリメントし、
     * 上限を超えていないか確認する。
     * 初回呼び出し時にTTLを設定し、期間経過後は自動的にカウントがリセットされる。
     *
     * @param key カウント対象を識別するキー
     * @param limit 期間内に許容する最大回数
     * @param window カウントをリセットするまでの期間
     * @return 上限を超えていなければtrue、超えていればfalse
     */
    public boolean tryAcquire(String key, long limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= limit;
    }
}
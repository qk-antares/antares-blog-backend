package org.example.antares.member.redis;

import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.constant.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Slf4j
public class PipelinedTest {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void test(){
        long start = System.currentTimeMillis();
        String cacheKeyPrefix = RedisConstants.NOTIFICATION_PREFIX + 2;
        List<String> keys = Arrays.asList(
                cacheKeyPrefix + RedisConstants.LIKE_NOTIFICATION_SUFFIX,
                cacheKeyPrefix + RedisConstants.COMMENT_NOTIFICATION_SUFFIX,
                cacheKeyPrefix + RedisConstants.MSG_NOTIFICATION_SUFFIX,
                cacheKeyPrefix + RedisConstants.NOTICE_NOTIFICATION_SUFFIX
        );

        List<Object> execute = stringRedisTemplate.executePipelined((RedisCallback<List<Object>>) connection -> {
            for (String key : keys) {
                connection.get(key.getBytes());
            }
            return null;
        });

        System.out.println(execute);
        long end = System.currentTimeMillis();
        log.info("耗时：{}", end - start);
    }
}

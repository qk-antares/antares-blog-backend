package com.antares.blog.service;

import com.antares.common.model.response.R;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    void testRedisson(){
        List<String> strings = new ArrayList<>();
        strings.add("hello");
        strings.add("world");
        System.out.println(strings.get(0));

        RList<String> rList = redissonClient.getList("test:list");
        rList.add("hello");
        rList.add("world");
        System.out.println(rList.get(0));

    }
}

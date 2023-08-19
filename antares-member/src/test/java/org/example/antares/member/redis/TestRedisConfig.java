package org.example.antares.member.redis;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.member.model.entity.User;
import org.example.antares.member.model.entity.UserTagCategory;
import org.example.antares.member.mapper.UserMapper;
import org.example.antares.member.mapper.UserTagCategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import static org.example.antares.common.constant.RedisConstants.USER_TAGS_CATEGORY;
import static org.example.antares.common.utils.ObjectMapperUtils.MAPPER;

@SpringBootTest
public class TestRedisConfig {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private UserTagCategoryMapper userTagCategoryMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testConfig(){
        User user = new User();
        user.setUid(1L);
        user.setUsername("324324");
        user.setPassword("");
        user.setTags("");
        user.setSignature("");
        user.setEmail("");
        user.setPhone("");
        user.setSex(0);
        user.setAvatar("");
        user.setSocialUid("");
        user.setAccessToken("");
        user.setExpiresIn(0L);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);

        redisTemplate.opsForValue().set("key2", user);
    }

    @Test
    void testLoad(){
        List<UserTagCategory> userTagCategories = userTagCategoryMapper.selectList(null);
        redisTemplate.opsForList().rightPushAll(USER_TAGS_CATEGORY, userTagCategories);
    }

    @Test
    void loadUser(){
        User user = userMapper.selectById(2L);
        redisTemplate.opsForValue().set("user:id:" + user.getUid(), user);
    }

    @Test
    void testObjectMapper(){
        try {
            UserInfoVo userInfoVo = MAPPER.readValue(stringRedisTemplate.opsForValue().get("token"), UserInfoVo.class);
            System.out.println(userInfoVo);
        } catch (JsonProcessingException e) {
        }
    }

    @Test
    void keys(){
        stringRedisTemplate.opsForValue().set("test:id:1:like", "1");
        stringRedisTemplate.opsForValue().set("test:id:1:like", "1");
        stringRedisTemplate.opsForValue().set("test:id:1:comment", "1");
        stringRedisTemplate.delete(Arrays.asList("test:id:1:like", "test:id:1:like", "test:id:1:comment"));
    }

}

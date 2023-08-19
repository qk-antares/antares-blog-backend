package org.example.antares.blog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.antares.blog.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;



@SpringBootTest
public class RedisTest {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private ArticleService articleService;


    @Test
    public void test () throws JsonProcessingException {
        // Article byId = articleService.getById(1);
        // redisTemplate.opsForValue().set(ARTICLE + "1", byId);
        // R articleById = articleService.getArticleById(2L);
        ObjectMapper mapper = new ObjectMapper();
        // ArticleResponse data = mapper.convertValue(articleById.get("data"), new TypeReference<ArticleResponse>() {});
        // String tagJson = mapper.writeValueAsString(data.getTags());
        // String timeJson = mapper.writeValueAsString(data.getUpdateTime());

        // stringRedisTemplate.opsForHash().put(ARTICLE + "1", "tags", tagJson);
        // stringRedisTemplate.opsForHash().put(ARTICLE + "1", "updateTime", timeJson);
    }

    @Test
    public void getArticle(){
        // Article result = (Article) redisTemplate.opsForValue().get(ARTICLE + "1");
        // System.out.println(result);
    }

    @Test
    public void testIncr(){
        //stringRedisTemplate.opsForValue().set("test", count.toString());
        //Long increment = stringRedisTemplate.opsForValue().increment("test");
        //System.out.println(increment);
        //Long increment0 = stringRedisTemplate.opsForValue().increment("test");
        //System.out.println(increment0);

        String tesdfs = stringRedisTemplate.opsForValue().get("tesdfs");
        System.out.println(Long.valueOf(null));
    }

    @Test
    void testKeys() {
        //stringRedisTemplate.keys("")
    }

}

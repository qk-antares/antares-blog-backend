package org.example.antares.member.job;

import org.example.antares.common.constant.SystemConstants;
import org.example.antares.member.mapper.UserMapper;
import org.example.antares.member.mapper.UserTagMapper;
import org.example.antares.member.model.entity.User;
import org.example.antares.member.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootTest
public class RecommendJobTest {
    @Resource
    private UserService userService;
    @Resource
    private UserTagMapper userTagMapper;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 要用sorted set存储
     */
    @Test
    public void getRecommendUsersJob(){
        long start = System.currentTimeMillis();

        int tagCount = userTagMapper.selectCount(null);
        List<User> userList = userService.lambdaQuery().select(User::getUid, User::getTags).list();
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

        int loopCount = userList.size() / SystemConstants.RANDOM_RECOMMEND_BATCH_SIZE;
        for (User user : userList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.getRecommendUserIdsAndCache(user.getUid(), user.getTags(), tagCount, loopCount);
            }, threadPoolExecutor);
            futures.add(future);
        }

        //缓存一个未登录用户，uid为-1
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            userService.getRandomUserIdsAndCache(tagCount, loopCount);
        }, threadPoolExecutor);
        futures.add(future);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long end = System.currentTimeMillis();
        System.out.println("执行耗时:" + (end - start));
    }

    @Test
    void testGet(){
        int tagCount = userTagMapper.selectCount(null);
        System.out.println(tagCount);

        //List<UserInfoVo> recommendUsers = userService.getRecommendUsers(null);
    }
}

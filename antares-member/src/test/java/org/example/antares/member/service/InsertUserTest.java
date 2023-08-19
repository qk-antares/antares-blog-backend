package org.example.antares.member.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.RandomUtils;
import org.example.antares.member.mapper.UserTagMapper;
import org.example.antares.member.model.entity.User;
import org.example.antares.member.model.entity.UserTag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@SpringBootTest
public class InsertUserTest {
    @Resource
    private UserService userService;
    @Resource
    private UserTagMapper userTagMapper;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Test
    public void insertUser(){
        String prefix = "antares_fake";
        List<UserTag> userTags = userTagMapper.selectList(null);
        int len = userTags.size();

        long start = System.currentTimeMillis();
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ArrayList<User> users = new ArrayList<>(100);
            for (int j = 0; j < 100; j++) {
                User user = new User();
                user.setUsername(prefix + String.format("%03d", i*100+j));
                user.setPassword("$2a$10$EqQgm9wDCXnhg6qgIv1JceLpLb4g9Cv5b");
                user.setEmail(user.getUsername() + "@126.com");

                //代表该用户的标签数
                int n = RandomUtils.nextInt(0, 11);

                if(n > 0){
                    //打乱userTags
                    for (int k = 0; k < len; k++) {
                        int index = RandomUtils.nextInt(0,len);
                        UserTag tmp = userTags.get(index);
                        userTags.set(index, userTags.get(k));
                        userTags.set(k, tmp);
                    }
                    user.setTags(JSON.toJSONString(userTags.subList(0, n)
                            .stream().map(UserTag::getId).collect(Collectors.toList())));
                }
                users.add(user);
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> userService.saveBatch(users), threadPoolExecutor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long end = System.currentTimeMillis();
        System.out.println("耗时" + (end-start));
    }

    @Test
    void testUserTagJSON(){
        User byId = userService.getById(2L);
        String tags = byId.getTags();
        int[] ints = JSON.parseObject(tags, new TypeReference<int[]>() {});
        System.out.println(ints);
    }

    @Test
    void random(){
        for (int i = 0; i < 100; i++) {
            System.out.println(RandomUtils.nextInt(0, 5));
        }
    }

    @Test
    void testJSON(){
        System.out.println(JSON.toJSONString(new ArrayList<Integer>(Arrays.asList(1,2,3))));
    }
}

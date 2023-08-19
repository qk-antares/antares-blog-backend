package org.example.antares.member.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.member.model.entity.Conversation;
import org.example.antares.member.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;

@SpringBootTest
@Slf4j
public class TestMP {
    @Resource
    private UserService userService;
    @Resource
    private ConversationService conversationService;

    @Test
    void testMP(){
        User one = userService.lambdaQuery().select(User::getPassword).eq(User::getUid, 2).one();
        System.out.println(one);
    }

    @Test
    void testUser(){
        ArrayList<Long> uids = new ArrayList<>();
        uids.add(2L);
        uids.add(3L);
        userService.getUserListByUids(uids, null);
    }

    @Test
    //耗时1594 3468
    //@Transactional
    void testUpdate(){
        long start = System.currentTimeMillis();
        conversationService.update(new UpdateWrapper<Conversation>()
                .eq("id", 3)
                .setSql("from_unread = from_unread + 1").eq("from_uid", 11));

        conversationService.update(new UpdateWrapper<Conversation>()
                .eq("id", 3)
                .setSql("to_unread = to_unread + 1").eq("to_uid", 11));
        long end = System.currentTimeMillis();
        log.info("耗时：{}", end - start);
    }
}

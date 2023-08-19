package org.example.antares.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class UserServiceTest {
    @Resource
    private UserService userService;
    @Test
    void testGetUserById(){
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            userService.getUserByUid(2L, null);
            long end = System.currentTimeMillis();
            System.out.println(end-start);
        }
    }
}

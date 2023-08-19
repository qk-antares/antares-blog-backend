package org.example.antares.member.util;

import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.example.antares.member.model.entity.User;
import org.example.antares.member.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class TestBeanCopyUtils {
    @Resource
    private UserMapper userMapper;

    @Test
    void testBeanCopyUtil(){
        User user = userMapper.selectById(2);
        UserInfoVo userInfoVo = BeanCopyUtils.copyBean(user, UserInfoVo.class);
        System.out.println(userInfoVo);
    }
}

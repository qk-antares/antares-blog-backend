package org.example.antares.blog.feign;

import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class UserFeignServiceTest {
    @Resource
    private UserFeignService userFeignService;

    @Test
    void testFeign(){
        R<UserInfoVo> response = userFeignService.info(2L);
        if (response.getCode() == AppHttpCodeEnum.SUCCESS.getCode()) {
            UserInfoVo author = response.getData();
            System.out.println(author);
        } else {
            throw new BusinessException(AppHttpCodeEnum.getEnumByCode(response.getCode()));
        }
    }
}

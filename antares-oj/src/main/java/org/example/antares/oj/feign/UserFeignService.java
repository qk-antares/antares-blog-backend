package org.example.antares.oj.feign;

import org.example.antares.common.model.dto.UsernameAndAvtarDto;
import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

@FeignClient("antares-member")
public interface UserFeignService {
    /**
     * 获取当前登录用户
     * @return
     */
    @GetMapping("/info")
    R<UserInfoVo> getCurrentUser();

    /**
     * 根据uid获取用户信息
     * @param uid
     * @return
     */
    @GetMapping("/member/info/{uid}")
    R<UserInfoVo> info(@PathVariable("uid") Long uid);
}
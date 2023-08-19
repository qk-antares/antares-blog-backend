package org.example.antares.search.feign;

import org.example.antares.common.model.vo.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("antares-member")
public interface UserFeignService {
    /**
     * 根据uid列表获取用户信息，
     * 根据uid列表获取用户信息，
     * @param uids
     * @return
     */
    @PostMapping("/member/info/list")
    public List<UserInfoVo> getUserListByUids(@RequestBody List<Long> uids);
}

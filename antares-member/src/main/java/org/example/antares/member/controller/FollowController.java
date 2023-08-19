package org.example.antares.member.controller;

import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.member.model.vo.user.FollowVo;
import org.example.antares.member.service.FollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/member")
public class FollowController {
    @Resource
    private FollowService followService;

    /**
     * 关注或取消关注
     * @param uid
     * @param request
     * @return
     */
    @PostMapping("/follow/{uid}")
    public R follow(@PathVariable("uid") Long uid, HttpServletRequest request){
        followService.follow(uid, request);
        return R.ok();
    }

    @GetMapping("/follows/of/current")
    public R getFollowsOfCurrent(HttpServletRequest request){
        List<FollowVo> follows = followService.getFollowsOfCurrent(request);
        return R.ok(follows);
    }

    @GetMapping("/follows/of/{uid}")
    public R getFollowsByUid(@PathVariable("uid") Long uid, HttpServletRequest request){
        List<UserInfoVo> follows = followService.getFollowsByUid(uid, request);
        return R.ok(follows);
    }

    @GetMapping("/fans/of/{uid}")
    public R getFansByUid(@PathVariable("uid") Long uid, HttpServletRequest request){
        List<UserInfoVo> fans = followService.getFansByUid(uid, request);
        return R.ok(fans);
    }
}

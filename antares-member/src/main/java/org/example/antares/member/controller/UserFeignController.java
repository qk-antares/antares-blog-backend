package org.example.antares.member.controller;

import org.example.antares.common.model.dto.UsernameAndAvtarDto;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.member.service.ConversationService;
import org.example.antares.member.service.FollowService;
import org.example.antares.member.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/member")
public class UserFeignController {
    @Resource
    private UserService userService;
    @Resource
    private FollowService followService;
    @Resource
    private ConversationService conversationService;

    @PostMapping("/list/username/avatar")
    public List<UsernameAndAvtarDto> getUsernameAndAvatarByUids(@RequestBody Collection<Long> uids){
        return userService.listUserNameAndAvatarByUids(uids);
    }

    @GetMapping("/{uid}/username/avatar")
    public UsernameAndAvtarDto getUsernameAndAvatar(@PathVariable("uid") Long uid){
        return userService.getUsernameAndAvatar(uid);
    }

    @GetMapping("/followIds/of/current")
    public List<Long> getFollowIdsOfCurrent(HttpServletRequest request){
        return followService.getFollowIdsOfCurrent(request);
    }

    /**
     * 根据uid列表获取用户信息，
     * 根据uid列表获取用户信息，
     * @param uids
     * @param request
     * @return
     */
    @PostMapping("/info/list")
    public List<UserInfoVo> getUserListByUids(@RequestBody List<Long> uids, HttpServletRequest request){
        return userService.getUserListByUids(uids, request);
    }

    @PostMapping("/conversation/clear")
    public void clearConversationUnread(@RequestParam("uid") Long uid){
        conversationService.clearConversationUnread(uid);
    }
}

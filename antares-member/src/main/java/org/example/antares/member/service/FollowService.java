package org.example.antares.member.service;

import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.member.model.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.member.model.vo.user.FollowVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【follow】的数据库操作Service
* @createDate 2023-05-15 15:15:38
*/
public interface FollowService extends IService<Follow> {

    void follow(Long uid, HttpServletRequest request);

    List<Long> getFollowIdsOfCurrent(HttpServletRequest request);

    List<FollowVo> getFollowsOfCurrent(HttpServletRequest request);

    List<UserInfoVo> getFollowsByUid(Long uid, HttpServletRequest request);

    List<UserInfoVo> getFansByUid(Long uid, HttpServletRequest request);
}

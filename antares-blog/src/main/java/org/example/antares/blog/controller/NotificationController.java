package org.example.antares.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.blog.model.dto.notification.NotificationQueryRequest;
import org.example.antares.blog.service.NotificationService;
import org.example.antares.common.model.response.R;
import org.example.antares.blog.model.vo.notification.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog/notification")
public class NotificationController {
    @Resource
    private NotificationService notificationService;

    /**
     * 用户登录后查询自己的通知数量，count保存在redis中，redis中没有了再去数据库查询
     * @param request
     * @return
     */
    @GetMapping("/count")
    public R getNoticeCount(HttpServletRequest request){
        NotificationCountVo notificationCountVo = notificationService.count(request);
        return R.ok(notificationCountVo);
    }

    @PostMapping("/clear")
    public R clearNotification(@RequestParam("type") String type, HttpServletRequest request){
        notificationService.clearNotification(type, request);
        return R.ok();
    }

    /**
     * 分页查询点赞消息记录
     * @param notificationQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/like")
    public R listLikeNotificationByPage(@RequestBody NotificationQueryRequest notificationQueryRequest,
                                    HttpServletRequest request){
        Page<LikeNotificationVo> page = notificationService.listLikeNotificationByPage(notificationQueryRequest, request);
        return R.ok(page);
    }

    /**
     * 分页查询评论消息记录
     * @param notificationQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/comment")
    public R listCommentNotificationByPage(@RequestBody NotificationQueryRequest notificationQueryRequest,
                                    HttpServletRequest request){
        Page<CommentNotificationVo> vos = notificationService.listCommentNotificationByPage(notificationQueryRequest, request);
        return R.ok(vos);
    }
}

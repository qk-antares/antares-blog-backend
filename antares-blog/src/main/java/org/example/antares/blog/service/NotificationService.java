package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.blog.model.dto.notification.NotificationQueryRequest;
import org.example.antares.blog.model.vo.notification.*;


import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Antares
 * @description 针对表【notification】的数据库操作Service
 * @createDate 2023-05-18 16:36:19
 */
public interface NotificationService {

    NotificationCountVo count(HttpServletRequest request);

    Page<LikeNotificationVo> listLikeNotificationByPage(NotificationQueryRequest notificationQueryRequest, HttpServletRequest request);

    void clearNotification(String type, HttpServletRequest request);

    Page<CommentNotificationVo> listCommentNotificationByPage(NotificationQueryRequest notificationQueryRequest, HttpServletRequest request);
}

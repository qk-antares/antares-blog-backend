package com.antares.blog.model.vo.notification;

import lombok.Data;

import java.util.Date;

@Data
public class CommentNotificationVo {
    private Long id;
    private Long fromUid;
    private String avatar;
    private String fromUsername;
    private String fromContent;
    private String content;
    private Long articleId;
    private String title;
    private String summary;
    private Date createTime;
}

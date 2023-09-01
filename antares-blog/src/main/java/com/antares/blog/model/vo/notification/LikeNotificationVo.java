package com.antares.blog.model.vo.notification;

import lombok.Data;

import java.util.Date;

@Data
public class LikeNotificationVo {
    private Long id;
    private Long fromUid;
    private String fromUsername;
    private String avatar;
    private String title;
    private String summary;
    private Long articleId;
    private Date createTime;
}

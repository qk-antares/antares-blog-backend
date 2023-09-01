package com.antares.blog.model.vo.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationCountVo {
    private Integer likeCount;
    private Integer commentCount;
    private Integer msgCount;
    private Integer noticeCount;
}

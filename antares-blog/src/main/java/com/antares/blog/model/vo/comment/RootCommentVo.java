package com.antares.blog.model.vo.comment;

import lombok.Data;

import java.util.Date;

@Data
public class RootCommentVo {
    private Long id;
    private String content;
    private Long fromUid;
    private String avatar;
    private String fromUsername;
    private Integer likeCount;
    private Date createTime;
    private Integer replyCount;
}

package org.example.antares.blog.model.vo.comment;

import lombok.Data;

import java.util.Date;

@Data
public class ChildrenCommentVo {
    private Long id;
    private Long articleId;
    private Long rootId;
    private String content;
    private Long fromUid;
    private String fromUsername;
    private String avatar;

    private Long toUid;
    private String toUsername;

    private Long toCommentId;
    private Integer likeCount;
    private Date createTime;
}

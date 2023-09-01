package com.antares.blog.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName article_comment
 */
@TableName(value ="article_comment")
@Data
public class ArticleComment implements Serializable {
    @TableId
    private Long id;
    private Long articleId;
    private Long rootId;
    private String content;
    private Long fromUid;
    private Long toUid;
    private Long toCommentId;
    private Integer likeCount;
    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
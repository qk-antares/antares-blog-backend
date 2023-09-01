package com.antares.blog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 文章和文章标签关联表
 * @TableName article_tag_relation
 */
@TableName(value ="article_tag_relation")
@Data
public class ArticleTagRelation implements Serializable {
    /**
     * 主键
     */
    private Long tagId;

    /**
     * 主键
     */
    private Long articleId;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}